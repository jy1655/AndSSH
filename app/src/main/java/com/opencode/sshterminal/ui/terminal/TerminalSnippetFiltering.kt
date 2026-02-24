package com.opencode.sshterminal.ui.terminal

import com.opencode.sshterminal.data.TerminalSnippet

internal fun filterTerminalSnippets(
    snippets: List<TerminalSnippet>,
    searchQuery: String,
): List<TerminalSnippet> {
    val query = searchQuery.trim()
    if (query.isBlank()) return snippets
    return snippets.filter { snippet ->
        snippet.title.contains(query, ignoreCase = true) ||
            snippet.command.contains(query, ignoreCase = true)
    }
}
