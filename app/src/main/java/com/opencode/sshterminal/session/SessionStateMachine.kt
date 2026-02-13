package com.opencode.sshterminal.session

import com.opencode.sshterminal.ssh.SshClient
import com.opencode.sshterminal.ssh.HostKeyChangedException
import com.opencode.sshterminal.ssh.SshSession
import com.opencode.sshterminal.terminal.TerminalEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionStateMachine(
    private val sshClient: SshClient,
    private val terminalEngine: TerminalEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow(
        SessionSnapshot(
            sessionId = SessionId(),
            state = SessionState.IDLE,
            host = "",
            port = 22,
            username = ""
        )
    )
    val snapshot: StateFlow<SessionSnapshot> = _snapshot.asStateFlow()

    private var activeSession: SshSession? = null
    private var pendingHostKeyRequest: ConnectRequest? = null

    fun connect(request: ConnectRequest) {
        scope.launch {
            _snapshot.value = SessionSnapshot(
                sessionId = SessionId(),
                state = SessionState.CONNECTING,
                host = request.host,
                port = request.port,
                username = request.username,
                hostKeyAlert = null
            )

            runCatching {
                runCatching { activeSession?.close() }
                val session = sshClient.connect(request)
                activeSession = session
                session.openPtyShell(request.termType, request.cols, request.rows)
                _snapshot.value = _snapshot.value.copy(state = SessionState.CONNECTED, error = null)

                session.readLoop { bytes ->
                    terminalEngine.feed(bytes)
                }
                _snapshot.value = _snapshot.value.copy(state = SessionState.DISCONNECTED)
            }.onFailure { err ->
                if (err is HostKeyChangedException) {
                    pendingHostKeyRequest = request
                    _snapshot.value = _snapshot.value.copy(
                        state = SessionState.FAILED,
                        error = err.message,
                        hostKeyAlert = HostKeyAlert(
                            host = err.host,
                            port = err.port,
                            fingerprint = err.fingerprint,
                            message = err.message
                        )
                    )
                } else {
                    _snapshot.value = _snapshot.value.copy(
                        state = SessionState.FAILED,
                        error = err.message ?: "unknown error"
                    )
                }
            }
        }
    }

    fun sendInput(bytes: ByteArray) {
        scope.launch {
            activeSession?.write(bytes)
        }
    }

    fun resize(cols: Int, rows: Int) {
        scope.launch {
            activeSession?.windowChange(cols, rows)
        }
    }

    fun disconnect() {
        scope.launch {
            runCatching { activeSession?.close() }
            activeSession = null
            _snapshot.value = _snapshot.value.copy(state = SessionState.DISCONNECTED)
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }

    fun dismissHostKeyAlert() {
        pendingHostKeyRequest = null
        _snapshot.value = _snapshot.value.copy(hostKeyAlert = null)
    }

    fun trustHostKeyOnce() {
        val request = pendingHostKeyRequest ?: return
        pendingHostKeyRequest = null
        _snapshot.value = _snapshot.value.copy(hostKeyAlert = null, error = null)
        connect(request.copy(hostKeyPolicy = HostKeyPolicy.TRUST_ONCE))
    }

    fun updateKnownHostsAndReconnect() {
        val request = pendingHostKeyRequest ?: return
        pendingHostKeyRequest = null
        _snapshot.value = _snapshot.value.copy(hostKeyAlert = null, error = null)
        connect(request.copy(hostKeyPolicy = HostKeyPolicy.UPDATE_KNOWN_HOSTS))
    }
}
