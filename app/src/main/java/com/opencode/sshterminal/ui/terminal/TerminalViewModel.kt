package com.opencode.sshterminal.ui.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.opencode.sshterminal.data.ConnectionProfile
import com.opencode.sshterminal.data.ConnectionRepository
import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.SessionManager
import com.opencode.sshterminal.session.SessionSnapshot
import com.opencode.sshterminal.session.SessionState
import com.opencode.sshterminal.terminal.TermuxTerminalBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val connectionRepository: ConnectionRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val connectionId: String = savedStateHandle["connectionId"] ?: ""

    val bridge = TermuxTerminalBridge(
        cols = 120,
        rows = 40,
        onWriteToSsh = { bytes -> sessionManager.sendInput(bytes) }
    )

    val snapshot: StateFlow<SessionSnapshot> = sessionManager.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessionManager.snapshot.value)

    init {
        viewModelScope.launch {
            sessionManager.outputBytes.collect { bytes ->
                bridge.feed(bytes)
            }
        }

        if (connectionId.isNotEmpty()) {
            viewModelScope.launch {
                val profile = connectionRepository.get(connectionId) ?: return@launch
                val shouldConnect = !sessionManager.isConnected || !isSameTarget(profile)
                if (shouldConnect) {
                    bridge.reset()
                    connectionRepository.touchLastUsed(profile.id)
                    sessionManager.connect(
                        ConnectRequest(
                            host = profile.host,
                            port = profile.port,
                            username = profile.username,
                            knownHostsPath = File(context.filesDir, "known_hosts").absolutePath,
                            password = profile.password,
                            privateKeyPath = profile.privateKeyPath,
                            cols = 120,
                            rows = 40
                        )
                    )
                }
            }
        }
    }

    fun sendInput(bytes: ByteArray) {
        sessionManager.sendInput(bytes)
    }

    fun sendText(text: String) {
        sessionManager.sendInput(text.toByteArray(Charsets.UTF_8))
    }

    fun resize(cols: Int, rows: Int) {
        bridge.resize(cols, rows)
        sessionManager.resize(cols, rows)
    }

    fun disconnect() {
        sessionManager.disconnect()
    }

    fun dismissHostKeyAlert() = sessionManager.dismissHostKeyAlert()
    fun trustHostKeyOnce() = sessionManager.trustHostKeyOnce()
    fun updateKnownHostsAndReconnect() = sessionManager.updateKnownHostsAndReconnect()

    val isConnected: Boolean get() = snapshot.value.state == SessionState.CONNECTED

    private fun isSameTarget(profile: ConnectionProfile): Boolean {
        val current = snapshot.value
        return current.host == profile.host &&
            current.port == profile.port &&
            current.username == profile.username
    }
}
