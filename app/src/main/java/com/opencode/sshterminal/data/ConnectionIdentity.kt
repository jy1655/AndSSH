package com.opencode.sshterminal.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ConnectionIdentity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String,
    val password: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyPassphrase: String? = null,
    val lastUsedEpochMillis: Long = 0L,
)
