package com.opencode.sshterminal.data

import kotlinx.serialization.Serializable

@Serializable
data class TerminalCommandHistoryEntry(
    val command: String,
    val usedAtEpochMillis: Long = System.currentTimeMillis(),
)
