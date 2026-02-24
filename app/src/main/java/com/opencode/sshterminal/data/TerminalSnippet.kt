package com.opencode.sshterminal.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TerminalSnippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val command: String,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
