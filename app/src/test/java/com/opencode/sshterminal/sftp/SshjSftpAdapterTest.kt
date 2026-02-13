package com.opencode.sshterminal.sftp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SshjSftpAdapterTest {
    @Test
    fun `returns true for ssh fx no such file message`() {
        assertTrue(isSftpNoSuchFileError(RuntimeException("SFTP error SSH_FX_NO_SUCH_FILE")))
    }

    @Test
    fun `returns true for plain no such file message`() {
        assertTrue(isSftpNoSuchFileError(RuntimeException("No such file")))
    }

    @Test
    fun `returns true when nested cause contains no such file`() {
        val t = RuntimeException("outer", RuntimeException("SSH_FX_NO_SUCH_FILE from server"))
        assertTrue(isSftpNoSuchFileError(t))
    }

    @Test
    fun `returns false for permission denied`() {
        assertFalse(isSftpNoSuchFileError(RuntimeException("Permission denied")))
    }

    @Test
    fun `returns false for auth failure`() {
        assertFalse(isSftpNoSuchFileError(RuntimeException("Authentication failed")))
    }
}
