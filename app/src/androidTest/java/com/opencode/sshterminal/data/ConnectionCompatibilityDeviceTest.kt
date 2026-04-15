package com.opencode.sshterminal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.sshterminal.security.EncryptionManager
import com.opencode.sshterminal.security.PasswordBasedEncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ConnectionCompatibilityDeviceTest {
    private val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun importBackup_marksLegacySecurityKeyProfilesAsUnsupported_onDevice() =
        runBlocking {
            val repository = createRepository("backup-import")
            val manager =
                ConnectionBackupManager(
                    connectionRepository = repository,
                    passwordBasedEncryptionManager = PasswordBasedEncryptionManager(),
                )
            val password = "device-test-password".toCharArray()
            try {
                val backupJson = createEncryptedLegacyBackup(password)

                val summary = manager.importBackup(backupJson = backupJson, password = password)
                val importedProfiles = repository.profiles.first()

                assertEquals(1, summary.profileCount)
                assertEquals(1, summary.unsupportedSecurityKeyProfileCount)
                assertEquals(1, importedProfiles.size)
                assertTrue(importedProfiles.single().hasUnsupportedSecurityKeyAuth)
            } finally {
                password.fill('\u0000')
            }
        }

    @Test
    fun repository_get_flagsLegacySecurityKeyProfiles_onDevice() =
        runBlocking {
            val dataStore = createDataStore("repo-decode")
            val encryptionManager = EncryptionManager()
            val repository =
                ConnectionRepository(
                    dataStore = dataStore,
                    encryptionManager = encryptionManager,
                )
            val id = "legacy-device-profile"
            val rawLegacyProfileJson =
                """
                {
                  "id": "$id",
                  "name": "legacy device profile",
                  "host": "device.example.com",
                  "port": 22,
                  "username": "android",
                  "securityKeyApplication": "ssh:device.example.com",
                  "securityKeyHandleBase64": "AAECAw==",
                  "securityKeyPublicKeyBase64": "BAUGBw==",
                  "securityKeyFlags": 1
                }
                """.trimIndent()

            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("conn_$id")] = encryptionManager.encrypt(rawLegacyProfileJson)
            }

            val profile = repository.get(id)

            assertNotNull(profile)
            assertTrue(requireNotNull(profile).hasUnsupportedSecurityKeyAuth)
        }

    @Test
    fun repository_get_ignoresNullLegacySecurityKeyPlaceholders_onDevice() =
        runBlocking {
            val dataStore = createDataStore("repo-null-placeholders")
            val encryptionManager = EncryptionManager()
            val repository =
                ConnectionRepository(
                    dataStore = dataStore,
                    encryptionManager = encryptionManager,
                )
            val id = "legacy-null-placeholder-profile"
            val rawLegacyProfileJson =
                """
                {
                  "id": "$id",
                  "name": "legacy null placeholder profile",
                  "host": "device.example.com",
                  "port": 22,
                  "username": "android",
                  "password": "secret",
                  "securityKeyApplication": null,
                  "securityKeyHandleBase64": null,
                  "securityKeyPublicKeyBase64": null,
                  "securityKeyFlags": 1
                }
                """.trimIndent()

            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("conn_$id")] = encryptionManager.encrypt(rawLegacyProfileJson)
            }

            val profile = repository.get(id)

            assertNotNull(profile)
            assertFalse(requireNotNull(profile).hasUnsupportedSecurityKeyAuth)
        }

    private fun createRepository(label: String): ConnectionRepository {
        return ConnectionRepository(
            dataStore = createDataStore(label),
            encryptionManager = EncryptionManager(),
        )
    }

    private fun createDataStore(label: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file =
            File(
                appContext.filesDir,
                "android-test-$label-${UUID.randomUUID()}.preferences_pb",
            )
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
    }

    private fun createEncryptedLegacyBackup(password: CharArray): String {
        val payloadJson =
            """
            {
              "exportedAtEpochMillis": 123,
              "profiles": [
                {
                  "id": "legacy-device-import",
                  "name": "legacy imported profile",
                  "host": "import.example.com",
                  "port": 22,
                  "username": "root",
                  "securityKeyApplication": "ssh:import.example.com",
                  "securityKeyHandleBase64": "AAECAw==",
                  "securityKeyPublicKeyBase64": "BAUGBw==",
                  "securityKeyFlags": 1
                }
              ],
              "identities": []
            }
            """.trimIndent()
        val passwordManager = PasswordBasedEncryptionManager()
        val ciphertext = passwordManager.encrypt(plaintext = payloadJson, password = password)
        return json.encodeToString(
            ConnectionBackupEnvelopeV2(
                format = ConnectionBackupManager.BACKUP_FORMAT_V2,
                ciphertext = ciphertext,
                kdf = "PBKDF2-HMAC-SHA256",
                iterations = PasswordBasedEncryptionManager.PBKDF2_ITERATIONS,
            ),
        )
    }
}
