package com.opencode.sshterminal.ui.theme

enum class ThemePreset(val id: String) {
    GREEN("green"),
    OCEAN("ocean"),
    SUNSET("sunset"),
    PURPLE("purple"),
    ;

    companion object {
        fun fromId(id: String): ThemePreset = entries.find { it.id == id } ?: GREEN
    }
}
