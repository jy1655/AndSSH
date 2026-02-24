package com.opencode.sshterminal.data

enum class TerminalShortcutLayoutItem(
    val id: String,
) {
    MENU("menu"),
    SNIPPETS("snippets"),
    HISTORY("history"),
    ESC("esc"),
    TAB("tab"),
    CTRL("ctrl"),
    ALT("alt"),
    ARROW_UP("arrow_up"),
    ARROW_DOWN("arrow_down"),
    ARROW_LEFT("arrow_left"),
    ARROW_RIGHT("arrow_right"),
    BACKSPACE("backspace"),
    PAGE_UP("page_up"),
    PAGE_DOWN("page_down"),
    CTRL_C("ctrl_c"),
    CTRL_D("ctrl_d"),
    PASTE("paste"),
    ;

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): TerminalShortcutLayoutItem? = byId[id]
    }
}

val DEFAULT_TERMINAL_SHORTCUT_LAYOUT_ITEMS: List<TerminalShortcutLayoutItem> =
    listOf(
        TerminalShortcutLayoutItem.MENU,
        TerminalShortcutLayoutItem.SNIPPETS,
        TerminalShortcutLayoutItem.HISTORY,
        TerminalShortcutLayoutItem.ESC,
        TerminalShortcutLayoutItem.TAB,
        TerminalShortcutLayoutItem.CTRL,
        TerminalShortcutLayoutItem.ALT,
        TerminalShortcutLayoutItem.ARROW_UP,
        TerminalShortcutLayoutItem.ARROW_DOWN,
        TerminalShortcutLayoutItem.ARROW_LEFT,
        TerminalShortcutLayoutItem.ARROW_RIGHT,
        TerminalShortcutLayoutItem.BACKSPACE,
        TerminalShortcutLayoutItem.PAGE_UP,
        TerminalShortcutLayoutItem.PAGE_DOWN,
        TerminalShortcutLayoutItem.CTRL_C,
        TerminalShortcutLayoutItem.CTRL_D,
        TerminalShortcutLayoutItem.PASTE,
    )

val DEFAULT_TERMINAL_SHORTCUT_LAYOUT: String =
    serializeTerminalShortcutLayout(DEFAULT_TERMINAL_SHORTCUT_LAYOUT_ITEMS)

fun parseTerminalShortcutLayout(layout: String?): List<TerminalShortcutLayoutItem> {
    val parsedItems =
        layout.orEmpty()
            .split(',')
            .mapNotNull { token -> TerminalShortcutLayoutItem.fromId(token.trim()) }
            .distinct()
    return if (parsedItems.isEmpty()) {
        DEFAULT_TERMINAL_SHORTCUT_LAYOUT_ITEMS
    } else {
        parsedItems
    }
}

fun serializeTerminalShortcutLayout(items: List<TerminalShortcutLayoutItem>): String {
    val normalized = items.distinct()
    return if (normalized.isEmpty()) {
        DEFAULT_TERMINAL_SHORTCUT_LAYOUT
    } else {
        normalized.joinToString(separator = ",") { item -> item.id }
    }
}
