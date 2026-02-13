package com.opencode.sshterminal.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SimpleTerminalEngine : TerminalEngine {
    private val _screen = MutableStateFlow("")
    override val screen: StateFlow<String> = _screen.asStateFlow()

    override fun feed(bytes: ByteArray) {
        _screen.value += bytes.decodeToString()
    }

    override fun clear() {
        _screen.value = ""
    }
}
