package com.opencode.sshterminal.ui.terminal

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.sshterminal.data.ConnectionProfile
import com.opencode.sshterminal.data.ConnectionRepository
import com.opencode.sshterminal.service.SshForegroundService
import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.SessionManager
import com.opencode.sshterminal.session.SessionSnapshot
import com.opencode.sshterminal.session.TabId
import com.opencode.sshterminal.session.TabInfo
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    val tabs: StateFlow<List<TabInfo>> = sessionManager.tabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessionManager.tabs.value)

    val activeTabId: StateFlow<TabId?> = sessionManager.activeTabId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessionManager.activeTabId.value)

    val activeSnapshot: StateFlow<SessionSnapshot?> = sessionManager.activeSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessionManager.activeSnapshot.value)

    val profiles: StateFlow<List<ConnectionProfile>> = connectionRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bridge: TermuxTerminalBridge get() = sessionManager.bridge

    init {
        viewModelScope.launch {
            sessionManager.hasAnyConnected.collect { anyConnected ->
                if (anyConnected) {
                    context.startForegroundService(
                        Intent(context, SshForegroundService::class.java)
                    )
                } else {
                    context.stopService(Intent(context, SshForegroundService::class.java))
                }
            }
        }
    }

    fun openTab(connectionId: String) {
        viewModelScope.launch {
            val profile = connectionRepository.get(connectionId) ?: return@launch
            connectionRepository.touchLastUsed(profile.id)
            sessionManager.openTab(
                title = profile.name,
                connectionId = profile.id,
                request = ConnectRequest(
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

    fun switchTab(tabId: TabId) = sessionManager.switchTab(tabId)

    fun closeTab(tabId: TabId) = sessionManager.closeTab(tabId)

    fun disconnectActiveTab() = sessionManager.disconnect()

    fun sendInput(bytes: ByteArray) = sessionManager.sendInput(bytes)

    fun sendText(text: String) = sessionManager.sendInput(text.toByteArray(Charsets.UTF_8))

    fun resize(cols: Int, rows: Int) = sessionManager.resize(cols, rows)

    fun forceRepaint() = sessionManager.forceRepaint()

    fun dismissHostKeyAlert() = sessionManager.dismissHostKeyAlert()
    fun trustHostKeyOnce() = sessionManager.trustHostKeyOnce()
    fun updateKnownHostsAndReconnect() = sessionManager.updateKnownHostsAndReconnect()

    val isConnected: Boolean get() = sessionManager.isConnected
}
