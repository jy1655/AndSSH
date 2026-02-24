package com.opencode.sshterminal.ui.terminal

import com.opencode.sshterminal.data.ConnectionProfile
import com.opencode.sshterminal.data.PortForwardRule
import com.opencode.sshterminal.data.PortForwardType
import com.opencode.sshterminal.session.SessionId
import com.opencode.sshterminal.session.SessionSnapshot
import com.opencode.sshterminal.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalConnectionInfoFormatterTest {
    @Test
    fun `returns null for blank host`() {
        val snapshot =
            SessionSnapshot(
                sessionId = SessionId("session-1"),
                state = SessionState.CONNECTED,
                host = "",
                port = 22,
                username = "dev",
            )

        val info = buildTerminalConnectionInfo(snapshot, profile = null)

        assertNull(info)
    }

    @Test
    fun `builds endpoint and counts`() {
        val snapshot =
            SessionSnapshot(
                sessionId = SessionId("session-2"),
                state = SessionState.CONNECTED,
                host = "example.com",
                port = 2222,
                username = "alice",
            )
        val profile =
            ConnectionProfile(
                id = "conn-1",
                name = "test",
                host = "example.com",
                port = 2222,
                username = "alice",
                proxyJump = "jump-a,jump-b",
                portForwards =
                    listOf(
                        PortForwardRule(
                            type = PortForwardType.DYNAMIC,
                            bindPort = 1080,
                        ),
                    ),
            )

        val info = buildTerminalConnectionInfo(snapshot, profile)

        assertEquals("alice@example.com:2222", info?.endpoint)
        assertEquals(2, info?.proxyJumpHopCount)
        assertEquals(1, info?.forwardCount)
    }

    @Test
    fun `formats only non-zero metadata lines`() {
        val text =
            TerminalConnectionInfo(
                endpoint = "u@h:22",
                proxyJumpHopCount = 1,
                forwardCount = 0,
            ).toDisplayText(
                proxyJumpFormatter = { count -> "PJ $count" },
                forwardFormatter = { count -> "FWD $count" },
            )

        assertEquals("u@h:22\nPJ 1", text)
    }
}
