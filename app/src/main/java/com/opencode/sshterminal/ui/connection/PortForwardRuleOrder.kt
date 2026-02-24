package com.opencode.sshterminal.ui.connection

import com.opencode.sshterminal.data.PortForwardRule

internal fun movePortForwardRule(
    rules: List<PortForwardRule>,
    fromIndex: Int,
    toIndex: Int,
): List<PortForwardRule> {
    if (fromIndex !in rules.indices) return rules
    if (toIndex !in rules.indices) return rules
    if (fromIndex == toIndex) return rules

    val reordered = rules.toMutableList()
    val moved = reordered.removeAt(fromIndex)
    reordered.add(toIndex, moved)
    return reordered
}
