package com.opencode.sshterminal.data

import kotlinx.serialization.Serializable

@Serializable
data class PortForwardRule(
    val type: PortForwardType,
    val bindHost: String? = null,
    val bindPort: Int,
    val targetHost: String? = null,
    val targetPort: Int? = null,
)

@Serializable
enum class PortForwardType {
    LOCAL,
    REMOTE,
    DYNAMIC,
}
