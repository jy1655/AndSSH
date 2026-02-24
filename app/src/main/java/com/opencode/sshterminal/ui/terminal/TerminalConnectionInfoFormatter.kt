package com.opencode.sshterminal.ui.terminal

import com.opencode.sshterminal.data.ConnectionProfile
import com.opencode.sshterminal.data.parseProxyJumpEntries
import com.opencode.sshterminal.session.SessionSnapshot

internal data class TerminalConnectionInfo(
    val endpoint: String,
    val proxyJumpHopCount: Int,
    val forwardCount: Int,
)

internal fun buildTerminalConnectionInfo(
    snapshot: SessionSnapshot?,
    profile: ConnectionProfile?,
): TerminalConnectionInfo? {
    val current = snapshot ?: return null
    if (current.host.isBlank()) return null

    val proxyJumpHopCount =
        profile?.proxyJump
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(::parseProxyJumpEntries)
            ?.size ?: 0
    val forwardCount = profile?.portForwards?.size ?: 0

    return TerminalConnectionInfo(
        endpoint = "${current.username}@${current.host}:${current.port}",
        proxyJumpHopCount = proxyJumpHopCount,
        forwardCount = forwardCount,
    )
}

internal fun TerminalConnectionInfo.toDisplayText(
    proxyJumpFormatter: (Int) -> String,
    forwardFormatter: (Int) -> String,
): String {
    val lines = mutableListOf(endpoint)
    if (proxyJumpHopCount > 0) {
        lines += proxyJumpFormatter(proxyJumpHopCount)
    }
    if (forwardCount > 0) {
        lines += forwardFormatter(forwardCount)
    }
    return lines.joinToString("\n")
}
