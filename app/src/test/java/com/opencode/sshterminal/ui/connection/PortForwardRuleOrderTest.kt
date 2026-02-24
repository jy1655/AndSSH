package com.opencode.sshterminal.ui.connection

import com.opencode.sshterminal.data.PortForwardRule
import com.opencode.sshterminal.data.PortForwardType
import org.junit.Assert.assertEquals
import org.junit.Test

class PortForwardRuleOrderTest {
    private val first =
        PortForwardRule(
            type = PortForwardType.LOCAL,
            bindPort = 8001,
            targetHost = "127.0.0.1",
            targetPort = 1,
        )
    private val second =
        PortForwardRule(
            type = PortForwardType.LOCAL,
            bindPort = 8002,
            targetHost = "127.0.0.1",
            targetPort = 2,
        )
    private val third =
        PortForwardRule(
            type = PortForwardType.LOCAL,
            bindPort = 8003,
            targetHost = "127.0.0.1",
            targetPort = 3,
        )

    @Test
    fun `moves rule upward`() {
        val rules = listOf(first, second, third)

        val moved = movePortForwardRule(rules, fromIndex = 2, toIndex = 1)

        assertEquals(listOf(first, third, second), moved)
    }

    @Test
    fun `moves rule downward`() {
        val rules = listOf(first, second, third)

        val moved = movePortForwardRule(rules, fromIndex = 0, toIndex = 1)

        assertEquals(listOf(second, first, third), moved)
    }

    @Test
    fun `returns same list for out of range index`() {
        val rules = listOf(first, second)

        val moved = movePortForwardRule(rules, fromIndex = 5, toIndex = 0)

        assertEquals(rules, moved)
    }
}
