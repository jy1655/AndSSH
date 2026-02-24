package com.opencode.sshterminal.ui.sftp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SftpPermissionOctalTest {
    @Test
    fun `parse accepts three-digit octal`() {
        assertEquals(420, parsePermissionOctal("644"))
        assertEquals(493, parsePermissionOctal("755"))
    }

    @Test
    fun `parse rejects invalid input`() {
        assertNull(parsePermissionOctal(""))
        assertNull(parsePermissionOctal("64"))
        assertNull(parsePermissionOctal("0644"))
        assertNull(parsePermissionOctal("888"))
        assertNull(parsePermissionOctal("abc"))
    }

    @Test
    fun `format returns zero padded octal`() {
        assertEquals("644", formatPermissionOctal(420))
        assertEquals("007", formatPermissionOctal(7))
    }

    @Test
    fun `format masks high bits`() {
        assertEquals("777", formatPermissionOctal(1023))
    }
}
