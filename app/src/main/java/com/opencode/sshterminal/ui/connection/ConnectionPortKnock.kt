package com.opencode.sshterminal.ui.connection

internal fun parsePortKnockSequenceInput(input: String): List<Int> {
    val deduped = linkedSetOf<Int>()
    input
        .split(',', ';', '\n', '\t', ' ')
        .map { token -> token.trim() }
        .filter { token -> token.isNotEmpty() }
        .forEach { token ->
            token.toIntOrNull()
                ?.takeIf { port -> port in 1..65535 }
                ?.let(deduped::add)
        }
    return deduped.toList()
}

internal fun formatPortKnockSequenceInput(sequence: List<Int>): String {
    if (sequence.isEmpty()) return ""
    return sequence.joinToString(",")
}
