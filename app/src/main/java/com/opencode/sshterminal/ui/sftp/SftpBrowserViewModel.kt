package com.opencode.sshterminal.ui.sftp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.sshterminal.data.ConnectionRepository
import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.sftp.RemoteEntry
import com.opencode.sshterminal.sftp.SftpChannelAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SftpUiState(
    val entries: List<RemoteEntry> = emptyList(),
    val remotePath: String = ".",
    val status: String = "",
    val busy: Boolean = false
)

@HiltViewModel
class SftpBrowserViewModel @Inject constructor(
    private val sftpAdapter: SftpChannelAdapter,
    private val connectionRepository: ConnectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""

    private val _uiState = MutableStateFlow(SftpUiState())
    val uiState: StateFlow<SftpUiState> = _uiState.asStateFlow()

    private var connectRequest: ConnectRequest? = null

    init {
        viewModelScope.launch {
            val profile = connectionRepository.get(connectionId) ?: return@launch
            connectRequest = ConnectRequest(
                host = profile.host,
                port = profile.port,
                username = profile.username,
                knownHostsPath = profile.knownHostsPath,
                privateKeyPath = profile.privateKeyPath,
                cols = 80,
                rows = 24
            )
        }
    }

    fun list(path: String = _uiState.value.remotePath) {
        val request = connectRequest ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, status = "Listing $path ...", remotePath = path)
            runCatching {
                val entries = sftpAdapter.list(request, path)
                _uiState.value = _uiState.value.copy(entries = entries, status = "Listed ${entries.size} entries", busy = false)
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(status = "List failed: ${t.message}", busy = false)
            }
        }
    }

    fun download(remotePath: String, localPath: String) {
        val request = connectRequest ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, status = "Downloading...")
            runCatching {
                sftpAdapter.download(request, remotePath, localPath)
                _uiState.value = _uiState.value.copy(status = "Downloaded to $localPath", busy = false)
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(status = "Download failed: ${t.message}", busy = false)
            }
        }
    }

    fun upload(localPath: String, remotePath: String) {
        val request = connectRequest ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, status = "Uploading...")
            runCatching {
                sftpAdapter.upload(request, localPath, remotePath)
                _uiState.value = _uiState.value.copy(status = "Upload completed", busy = false)
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(status = "Upload failed: ${t.message}", busy = false)
            }
        }
    }

    fun navigateTo(path: String) {
        list(path)
    }

    fun setRemotePath(path: String) {
        _uiState.value = _uiState.value.copy(remotePath = path)
    }
}
