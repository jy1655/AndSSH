package com.opencode.sshterminal.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionProfileAuthStateTest {
    @Test
    fun `unsupported security key auth does not block when password is available`() {
        val profile =
            ConnectionProfile(
                id = "profile-1",
                name = "password fallback",
                host = "example.com",
                username = "dev",
                password = "secret",
                hasUnsupportedSecurityKeyAuth = true,
            )

        assertFalse(profile.hasBlockingUnsupportedSecurityKeyAuth())
    }

    @Test
    fun `unsupported security key auth blocks when no usable auth remains`() {
        val profile =
            ConnectionProfile(
                id = "profile-2",
                name = "legacy security key",
                host = "example.com",
                username = "dev",
                hasUnsupportedSecurityKeyAuth = true,
            )

        assertTrue(profile.hasBlockingUnsupportedSecurityKeyAuth())
    }

    @Test
    fun `private key relink does not block when identity has password fallback`() {
        val profile =
            ConnectionProfile(
                id = "profile-3",
                name = "identity fallback",
                host = "example.com",
                username = "dev",
                requiresPrivateKeyRelink = true,
            )
        val identity =
            ConnectionIdentity(
                id = "identity-1",
                name = "identity",
                username = "dev",
                password = "secret",
            )

        assertFalse(profile.hasBlockingPrivateKeyRelink(identity))
    }
}
