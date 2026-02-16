package com.opencode.sshterminal.ui.opencode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.sshterminal.session.SessionManager
import com.opencode.sshterminal.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenCodeUiState(
    val output: String = "",
    val isProcessing: Boolean = false,
    val isConnected: Boolean = false,
    val history: List<PromptEntry> = emptyList()
)

data class PromptEntry(
    val prompt: String,
    val response: String
)

@HiltViewModel
class OpenCodeViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenCodeUiState())
    val uiState: StateFlow<OpenCodeUiState> = _uiState.asStateFlow()

    private val outputBuffer = StringBuilder()

    init {
        viewModelScope.launch {
            sessionManager.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    isConnected = snapshot.state == SessionState.CONNECTED
                )
            }
        }

        viewModelScope.launch {
            sessionManager.outputBytes.collect { bytes ->
                val text = bytes.decodeToString()
                outputBuffer.append(text)
                _uiState.value = _uiState.value.copy(
                    output = outputBuffer.toString(),
                    isProcessing = false
                )
            }
        }
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        _uiState.value = _uiState.value.copy(isProcessing = true)
        outputBuffer.clear()
        sessionManager.sendInput("$prompt\r".toByteArray(Charsets.UTF_8))

        val history = _uiState.value.history + PromptEntry(prompt = prompt, response = "")
        _uiState.value = _uiState.value.copy(history = history)
    }

    fun clearOutput() {
        outputBuffer.clear()
        _uiState.value = _uiState.value.copy(output = "")
    }
}
