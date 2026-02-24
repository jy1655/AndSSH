package com.opencode.sshterminal.terminal

import com.termux.terminal.TerminalEmulator

enum class TerminalColorSchemePreset(
    val id: String,
    val displayName: String,
    val ansiColors: IntArray,
    val foreground: Int,
    val background: Int,
    val cursor: Int,
) {
    DEFAULT(
        id = "default",
        displayName = "Default",
        ansiColors =
            intArrayOf(
                rgb(0x000000), rgb(0xCD0000), rgb(0x00CD00), rgb(0xCDCD00),
                rgb(0x0000EE), rgb(0xCD00CD), rgb(0x00CDCD), rgb(0xE5E5E5),
                rgb(0x7F7F7F), rgb(0xFF0000), rgb(0x00FF00), rgb(0xFFFF00),
                rgb(0x5C5CFF), rgb(0xFF00FF), rgb(0x00FFFF), rgb(0xFFFFFF),
            ),
        foreground = rgb(0xE5E5E5),
        background = rgb(0x000000),
        cursor = rgb(0xE5E5E5),
    ),
    SOLARIZED_DARK(
        id = "solarized_dark",
        displayName = "Solarized Dark",
        ansiColors =
            intArrayOf(
                rgb(0x073642), rgb(0xDC322F), rgb(0x859900), rgb(0xB58900),
                rgb(0x268BD2), rgb(0xD33682), rgb(0x2AA198), rgb(0xEEE8D5),
                rgb(0x002B36), rgb(0xCB4B16), rgb(0x586E75), rgb(0x657B83),
                rgb(0x839496), rgb(0x6C71C4), rgb(0x93A1A1), rgb(0xFDF6E3),
            ),
        foreground = rgb(0x839496),
        background = rgb(0x002B36),
        cursor = rgb(0x93A1A1),
    ),
    SOLARIZED_LIGHT(
        id = "solarized_light",
        displayName = "Solarized Light",
        ansiColors =
            intArrayOf(
                rgb(0x073642), rgb(0xDC322F), rgb(0x859900), rgb(0xB58900),
                rgb(0x268BD2), rgb(0xD33682), rgb(0x2AA198), rgb(0xEEE8D5),
                rgb(0x002B36), rgb(0xCB4B16), rgb(0x586E75), rgb(0x657B83),
                rgb(0x839496), rgb(0x6C71C4), rgb(0x93A1A1), rgb(0xFDF6E3),
            ),
        foreground = rgb(0x657B83),
        background = rgb(0xFDF6E3),
        cursor = rgb(0x586E75),
    ),
    DRACULA(
        id = "dracula",
        displayName = "Dracula",
        ansiColors =
            intArrayOf(
                rgb(0x21222C), rgb(0xFF5555), rgb(0x50FA7B), rgb(0xF1FA8C),
                rgb(0xBD93F9), rgb(0xFF79C6), rgb(0x8BE9FD), rgb(0xF8F8F2),
                rgb(0x6272A4), rgb(0xFF6E6E), rgb(0x69FF94), rgb(0xFFFFA5),
                rgb(0xD6ACFF), rgb(0xFF92DF), rgb(0xA4FFFF), rgb(0xFFFFFF),
            ),
        foreground = rgb(0xF8F8F2),
        background = rgb(0x282A36),
        cursor = rgb(0xF8F8F2),
    ),
    NORD(
        id = "nord",
        displayName = "Nord",
        ansiColors =
            intArrayOf(
                rgb(0x3B4252), rgb(0xBF616A), rgb(0xA3BE8C), rgb(0xEBCB8B),
                rgb(0x81A1C1), rgb(0xB48EAD), rgb(0x88C0D0), rgb(0xE5E9F0),
                rgb(0x4C566A), rgb(0xBF616A), rgb(0xA3BE8C), rgb(0xEBCB8B),
                rgb(0x81A1C1), rgb(0xB48EAD), rgb(0x8FBCBB), rgb(0xECEFF4),
            ),
        foreground = rgb(0xD8DEE9),
        background = rgb(0x2E3440),
        cursor = rgb(0xD8DEE9),
    ),
    MONOKAI(
        id = "monokai",
        displayName = "Monokai",
        ansiColors =
            intArrayOf(
                rgb(0x272822), rgb(0xF92672), rgb(0xA6E22E), rgb(0xF4BF75),
                rgb(0x66D9EF), rgb(0xAE81FF), rgb(0xA1EFE4), rgb(0xF8F8F2),
                rgb(0x75715E), rgb(0xF92672), rgb(0xA6E22E), rgb(0xF4BF75),
                rgb(0x66D9EF), rgb(0xAE81FF), rgb(0xA1EFE4), rgb(0xF9F8F5),
            ),
        foreground = rgb(0xF8F8F2),
        background = rgb(0x272822),
        cursor = rgb(0xF8F8F0),
    ),
    GRUVBOX_DARK(
        id = "gruvbox_dark",
        displayName = "Gruvbox Dark",
        ansiColors =
            intArrayOf(
                rgb(0x282828), rgb(0xCC241D), rgb(0x98971A), rgb(0xD79921),
                rgb(0x458588), rgb(0xB16286), rgb(0x689D6A), rgb(0xA89984),
                rgb(0x928374), rgb(0xFB4934), rgb(0xB8BB26), rgb(0xFABD2F),
                rgb(0x83A598), rgb(0xD3869B), rgb(0x8EC07C), rgb(0xEBDBB2),
            ),
        foreground = rgb(0xEBDBB2),
        background = rgb(0x282828),
        cursor = rgb(0xEBDBB2),
    ),
    ONE_DARK(
        id = "one_dark",
        displayName = "One Dark",
        ansiColors =
            intArrayOf(
                rgb(0x282C34), rgb(0xE06C75), rgb(0x98C379), rgb(0xE5C07B),
                rgb(0x61AFEF), rgb(0xC678DD), rgb(0x56B6C2), rgb(0xABB2BF),
                rgb(0x5C6370), rgb(0xE06C75), rgb(0x98C379), rgb(0xE5C07B),
                rgb(0x61AFEF), rgb(0xC678DD), rgb(0x56B6C2), rgb(0xFFFFFF),
            ),
        foreground = rgb(0xABB2BF),
        background = rgb(0x282C34),
        cursor = rgb(0xABB2BF),
    ),
    TOKYO_NIGHT(
        id = "tokyo_night",
        displayName = "Tokyo Night",
        ansiColors =
            intArrayOf(
                rgb(0x1D202F), rgb(0xF7768E), rgb(0x9ECE6A), rgb(0xE0AF68),
                rgb(0x7AA2F7), rgb(0xBB9AF7), rgb(0x7DCFFF), rgb(0xA9B1D6),
                rgb(0x414868), rgb(0xF7768E), rgb(0x9ECE6A), rgb(0xE0AF68),
                rgb(0x7AA2F7), rgb(0xBB9AF7), rgb(0x7DCFFF), rgb(0xC0CAF5),
            ),
        foreground = rgb(0xC0CAF5),
        background = rgb(0x1A1B26),
        cursor = rgb(0xC0CAF5),
    ),
    CATPPUCCIN_MOCHA(
        id = "catppuccin_mocha",
        displayName = "Catppuccin Mocha",
        ansiColors =
            intArrayOf(
                rgb(0x45475A), rgb(0xF38BA8), rgb(0xA6E3A1), rgb(0xF9E2AF),
                rgb(0x89B4FA), rgb(0xF5C2E7), rgb(0x94E2D5), rgb(0xBAC2DE),
                rgb(0x585B70), rgb(0xF38BA8), rgb(0xA6E3A1), rgb(0xF9E2AF),
                rgb(0x89B4FA), rgb(0xF5C2E7), rgb(0x94E2D5), rgb(0xA6ADC8),
            ),
        foreground = rgb(0xCDD6F4),
        background = rgb(0x1E1E2E),
        cursor = rgb(0xF5E0DC),
    ),
    ;

    companion object {
        fun fromId(id: String): TerminalColorSchemePreset = entries.find { it.id == id } ?: DEFAULT
    }
}

fun applyColorScheme(
    emulator: TerminalEmulator,
    preset: TerminalColorSchemePreset,
) {
    val colors = emulator.mColors.mCurrentColors
    for (i in 0..15) {
        colors[i] = preset.ansiColors[i]
    }
    colors[256] = preset.foreground
    colors[257] = preset.background
}

private fun rgb(rgb: Int): Int = 0xFF000000.toInt() or rgb
