package com.opencode.sshterminal.ui.connection

import com.opencode.sshterminal.data.PortForwardRule
import com.opencode.sshterminal.data.PortForwardType

internal fun formatPortForwardRuleDisplay(rule: PortForwardRule): String {
    val bindHost = rule.bindHost ?: DEFAULT_BIND_HOST
    return when (rule.type) {
        PortForwardType.LOCAL -> {
            val targetHost = rule.targetHost ?: UNKNOWN_HOST
            val targetPort = rule.targetPort?.toString() ?: UNKNOWN_PORT
            "L $bindHost:${rule.bindPort} -> $targetHost:$targetPort"
        }
        PortForwardType.REMOTE -> {
            val targetHost = rule.targetHost ?: UNKNOWN_HOST
            val targetPort = rule.targetPort?.toString() ?: UNKNOWN_PORT
            "R $bindHost:${rule.bindPort} -> $targetHost:$targetPort"
        }
        PortForwardType.DYNAMIC -> "D $bindHost:${rule.bindPort}"
    }
}

private const val DEFAULT_BIND_HOST = "127.0.0.1"
private const val UNKNOWN_HOST = "?"
private const val UNKNOWN_PORT = "?"
