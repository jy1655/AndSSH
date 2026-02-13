package com.opencode.sshterminal.terminal

import kotlinx.coroutines.flow.StateFlow

interface TerminalEngine {
    val screen: StateFlow<String>
    fun feed(bytes: ByteArray)
    fun clear()
}
