package com.opencode.sshterminal.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionProfileCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `marks legacy security key profile as unsupported`() {
        val legacyJson =
            """
            {
              "id": "legacy-sk-1",
              "name": "legacy security key",
              "host": "legacy.example.com",
              "port": 22,
              "username": "root",
              "securityKeyApplication": "ssh:legacy.example.com",
              "securityKeyHandleBase64": "AAECAw==",
              "securityKeyPublicKeyBase64": "BAUGBw==",
              "securityKeyFlags": 1
            }
            """.trimIndent()

        val decoded =
            requireNotNull(
                ConnectionProfileCompatibility.decodeProfileOrNull(
                    json = json,
                    rawProfileJson = legacyJson,
                ),
            )

        assertEquals("legacy-sk-1", decoded.id)
        assertEquals("legacy.example.com", decoded.host)
        assertTrue(decoded.hasUnsupportedSecurityKeyAuth)
    }

    @Test
    fun `keeps non security key profile unflagged`() {
        val currentJson =
            """
            {
              "id": "plain-1",
              "name": "plain",
              "host": "example.com",
              "port": 22,
              "username": "dev",
              "password": "secret"
            }
            """.trimIndent()

        val decoded =
            requireNotNull(
                ConnectionProfileCompatibility.decodeProfileOrNull(
                    json = json,
                    rawProfileJson = currentJson,
                ),
            )

        assertFalse(decoded.hasUnsupportedSecurityKeyAuth)
    }

    @Test
    fun `ignores null legacy security key placeholders`() {
        val placeholderJson =
            """
            {
              "id": "placeholder-1",
              "name": "placeholder",
              "host": "example.com",
              "port": 22,
              "username": "dev",
              "password": "secret",
              "securityKeyApplication": null,
              "securityKeyHandleBase64": null,
              "securityKeyPublicKeyBase64": null,
              "securityKeyFlags": 1
            }
            """.trimIndent()

        val decoded =
            requireNotNull(
                ConnectionProfileCompatibility.decodeProfileOrNull(
                    json = json,
                    rawProfileJson = placeholderJson,
                ),
            )

        assertFalse(decoded.hasUnsupportedSecurityKeyAuth)
    }

    @Test
    fun `counts unsupported legacy security key profiles in backup payload`() {
        val backupPayloadJson =
            """
            {
              "exportedAtEpochMillis": 123,
              "profiles": [
                {
                  "id": "legacy-sk-1",
                  "name": "legacy security key",
                  "host": "legacy.example.com",
                  "port": 22,
                  "username": "root",
                  "securityKeyApplication": "ssh:legacy.example.com",
                  "securityKeyHandleBase64": "AAECAw==",
                  "securityKeyPublicKeyBase64": "BAUGBw==",
                  "securityKeyFlags": 1
                },
                {
                  "id": "plain-1",
                  "name": "plain",
                  "host": "example.com",
                  "port": 22,
                  "username": "dev",
                  "password": "secret"
                }
              ],
              "identities": []
            }
            """.trimIndent()

        val parsed =
            ConnectionProfileCompatibility.parseBackupPayload(
                json = json,
                rawPayloadJson = backupPayloadJson,
            )

        assertEquals(2, parsed.profiles.size)
        assertEquals(1, parsed.unsupportedSecurityKeyProfileCount)
        assertTrue(parsed.profiles.first().hasUnsupportedSecurityKeyAuth)
        assertFalse(parsed.profiles.last().hasUnsupportedSecurityKeyAuth)
    }
}
