package com.opencode.sshterminal.ui.connection

import com.opencode.sshterminal.data.PortForwardRule
import com.opencode.sshterminal.data.PortForwardType
import org.junit.Assert.assertEquals
import org.junit.Test

class PortForwardDisplayFormatterTest {
    @Test
    fun `formats local forward`() {
        val display =
            formatPortForwardRuleDisplay(
                PortForwardRule(
                    type = PortForwardType.LOCAL,
                    bindHost = "0.0.0.0",
                    bindPort = 8080,
                    targetHost = "127.0.0.1",
                    targetPort = 80,
                ),
            )

        assertEquals("L 0.0.0.0:8080 -> 127.0.0.1:80", display)
    }

    @Test
    fun `formats remote forward with default bind host`() {
        val display =
            formatPortForwardRuleDisplay(
                PortForwardRule(
                    type = PortForwardType.REMOTE,
                    bindPort = 2222,
                    targetHost = "localhost",
                    targetPort = 22,
                ),
            )

        assertEquals("R 127.0.0.1:2222 -> localhost:22", display)
    }

    @Test
    fun `formats dynamic forward`() {
        val display =
            formatPortForwardRuleDisplay(
                PortForwardRule(
                    type = PortForwardType.DYNAMIC,
                    bindPort = 1080,
                ),
            )

        assertEquals("D 127.0.0.1:1080", display)
    }
}
