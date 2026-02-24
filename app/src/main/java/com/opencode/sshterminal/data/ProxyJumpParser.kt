package com.opencode.sshterminal.data

data class ProxyJumpEntry(
    val username: String?,
    val host: String,
    val port: Int,
)

fun parseProxyJumpEntries(proxyJump: String): List<ProxyJumpEntry> {
    return proxyJump.split(',')
        .mapNotNull { token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            parseProxyJumpEntry(trimmed)
        }
}

fun proxyJumpHostPortKey(
    host: String,
    port: Int,
): String = "$host:$port"

private fun parseProxyJumpEntry(token: String): ProxyJumpEntry? {
    val atIndex = token.indexOf('@')
    val username: String?
    val hostPortPart: String
    if (atIndex > 0 && atIndex < token.lastIndex) {
        username = token.substring(0, atIndex).trim().ifBlank { null }
        hostPortPart = token.substring(atIndex + 1)
    } else {
        username = null
        hostPortPart = token
    }

    val hostPort = parseHostAndPort(hostPortPart, DEFAULT_SSH_PORT) ?: return null
    return ProxyJumpEntry(
        username = username,
        host = hostPort.first,
        port = hostPort.second,
    )
}

private fun parseHostAndPort(
    value: String,
    defaultPort: Int,
): Pair<String, Int>? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("[") && "]:" in trimmed) {
        val hostEnd = trimmed.indexOf("]:")
        if (hostEnd <= 1) return null
        val host = trimmed.substring(1, hostEnd)
        if (!isValidHostToken(host)) return null
        val port = trimmed.substring(hostEnd + 2).toIntOrNull() ?: return null
        return host to port
    }
    val lastColon = trimmed.lastIndexOf(':')
    if (lastColon > 0 && lastColon < trimmed.lastIndex) {
        val host = trimmed.substring(0, lastColon).trim()
        val port = trimmed.substring(lastColon + 1).toIntOrNull() ?: return null
        if (!isValidHostToken(host)) return null
        return host to port
    }
    if (!isValidHostToken(trimmed)) return null
    return trimmed to defaultPort
}

private fun isValidHostToken(host: String): Boolean {
    return host.isNotBlank() &&
        '@' !in host &&
        ' ' !in host &&
        '\t' !in host
}

private const val DEFAULT_SSH_PORT = 22
