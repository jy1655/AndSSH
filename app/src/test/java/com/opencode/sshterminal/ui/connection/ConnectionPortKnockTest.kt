package com.opencode.sshterminal.ui.connection

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionPortKnockTest {
    @Test
    fun `parse supports multiple separators and keeps order`() {
        val parsed = parsePortKnockSequenceInput("7000, 8000;9000\n10000")
        assertEquals(listOf(7000, 8000, 9000, 10000), parsed)
    }

    @Test
    fun `parse drops invalid and duplicate ports`() {
        val parsed = parsePortKnockSequenceInput("0,22,abc,22,65536,443")
        assertEquals(listOf(22, 443), parsed)
    }

    @Test
    fun `format joins ports with commas`() {
        assertEquals("7000,8000,9000", formatPortKnockSequenceInput(listOf(7000, 8000, 9000)))
    }
}
