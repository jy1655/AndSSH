package com.opencode.sshterminal.ui.terminal

import com.opencode.sshterminal.data.TerminalSnippet
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalSnippetFilteringTest {
    private val restartNginx =
        TerminalSnippet(
            id = "1",
            title = "Restart nginx",
            command = "sudo systemctl restart nginx",
            updatedAtEpochMillis = 30L,
        )
    private val tailAuthLog =
        TerminalSnippet(
            id = "2",
            title = "Tail auth log",
            command = "tail -f /var/log/auth.log",
            updatedAtEpochMillis = 20L,
        )
    private val diskUsage =
        TerminalSnippet(
            id = "3",
            title = "Disk usage",
            command = "df -h",
            updatedAtEpochMillis = 10L,
        )

    @Test
    fun `blank query returns all snippets`() {
        val snippets = listOf(restartNginx, tailAuthLog, diskUsage)

        val filtered = filterTerminalSnippets(snippets = snippets, searchQuery = "  ")

        assertEquals(snippets, filtered)
    }

    @Test
    fun `query matches title`() {
        val snippets = listOf(restartNginx, tailAuthLog, diskUsage)

        val filtered = filterTerminalSnippets(snippets = snippets, searchQuery = "tail")

        assertEquals(listOf(tailAuthLog), filtered)
    }

    @Test
    fun `query matches command`() {
        val snippets = listOf(restartNginx, tailAuthLog, diskUsage)

        val filtered = filterTerminalSnippets(snippets = snippets, searchQuery = "systemctl")

        assertEquals(listOf(restartNginx), filtered)
    }
}
