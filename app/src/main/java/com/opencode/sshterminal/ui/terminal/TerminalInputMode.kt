package com.opencode.sshterminal.ui.terminal

internal enum class TerminalInputMode(val id: String) {
    DIRECT("direct"),
    TEXT_BAR("text_bar"),
    ;

    companion object {
        fun fromId(id: String): TerminalInputMode = entries.firstOrNull { it.id == id } ?: DIRECT
    }
}
