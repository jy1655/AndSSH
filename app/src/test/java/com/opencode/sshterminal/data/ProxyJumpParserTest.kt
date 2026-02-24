package com.opencode.sshterminal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyJumpParserTest {
    @Test
    fun `parses username host and port`() {
        val entries = parseProxyJumpEntries("alice@bastion:2222,jump-2")
        assertEquals(2, entries.size)

        val first = entries[0]
        assertEquals("alice", first.username)
        assertEquals("bastion", first.host)
        assertEquals(2222, first.port)

        val second = entries[1]
        assertEquals(null, second.username)
        assertEquals("jump-2", second.host)
        assertEquals(22, second.port)
    }

    @Test
    fun `ignores invalid entries`() {
        val entries = parseProxyJumpEntries(" ,@@,bad:port,ok-host")
        assertEquals(1, entries.size)
        assertEquals("ok-host", entries[0].host)
    }

    @Test
    fun `builds stable host port key`() {
        assertEquals("bastion:22", proxyJumpHostPortKey("bastion", 22))
        assertTrue(proxyJumpHostPortKey("10.0.0.1", 10022).contains(":10022"))
    }
}
