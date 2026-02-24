package com.opencode.sshterminal.terminal

import com.opencode.sshterminal.R

enum class TerminalFontPreset(
    val id: String,
    val displayName: String,
    val fontResId: Int,
) {
    MESLO_NERD("meslo_nerd", "Meslo LGS NF", R.font.meslo_lgs_nf_regular),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", R.font.jetbrains_mono_regular),
    FIRA_CODE("fira_code", "Fira Code", R.font.fira_code_regular),
    HACK("hack", "Hack", R.font.hack_regular),
    SOURCE_CODE_PRO("source_code_pro", "Source Code Pro", R.font.source_code_pro_regular),
    ;

    companion object {
        fun fromId(id: String): TerminalFontPreset = entries.firstOrNull { it.id == id } ?: MESLO_NERD
    }
}
