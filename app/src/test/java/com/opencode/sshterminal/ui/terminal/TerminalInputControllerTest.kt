package com.opencode.sshterminal.ui.terminal

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalInputControllerTest {
    @Test
    fun `submitInput reports typed command`() {
        val sent = mutableListOf<ByteArray>()
        var submittedCommand: String? = null
        val controller =
            TerminalInputController(
                onSendBytes = { payload -> sent.add(payload) },
                onSubmitCommand = { command -> submittedCommand = command },
            )

        controller.onTextFieldValueChange(TextFieldValue("ls -la"))
        sent.clear()

        controller.submitInput()

        assertEquals("ls -la", submittedCommand)
        assertTrue(sent.single().contentEquals(byteArrayOf('\r'.code.toByte())))
    }

    @Test
    fun `submitInput skips blank command`() {
        val sent = mutableListOf<ByteArray>()
        var submittedCommand: String? = null
        val controller =
            TerminalInputController(
                onSendBytes = { payload -> sent.add(payload) },
                onSubmitCommand = { command -> submittedCommand = command },
            )

        controller.onTextFieldValueChange(TextFieldValue("   "))
        sent.clear()

        controller.submitInput()

        assertNull(submittedCommand)
        assertTrue(sent.single().contentEquals(byteArrayOf('\r'.code.toByte())))
    }
}
