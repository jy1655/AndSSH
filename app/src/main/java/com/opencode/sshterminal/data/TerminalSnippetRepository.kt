package com.opencode.sshterminal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opencode.sshterminal.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalSnippetRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val encryptionManager: EncryptionManager,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        val snippets: Flow<List<TerminalSnippet>> =
            dataStore.data.map { prefs ->
                prefs.asMap()
                    .filter { (key, _) -> key.name.startsWith(SNIPPET_KEY_PREFIX) }
                    .values
                    .mapNotNull { value -> (value as? String)?.let(::decodeSnippet) }
                    .sortedByDescending { snippet -> snippet.updatedAtEpochMillis }
            }

        suspend fun save(snippet: TerminalSnippet) {
            dataStore.edit { prefs ->
                prefs[keyForSnippet(snippet.id)] = encryptionManager.encrypt(json.encodeToString(snippet))
            }
        }

        suspend fun delete(id: String) {
            dataStore.edit { prefs ->
                prefs.remove(keyForSnippet(id))
            }
        }

        private fun decodeSnippet(raw: String): TerminalSnippet? {
            return runCatching {
                val decrypted = encryptionManager.decrypt(raw)
                json.decodeFromString<TerminalSnippet>(decrypted)
            }.getOrNull()
        }

        private fun keyForSnippet(id: String) = stringPreferencesKey("$SNIPPET_KEY_PREFIX$id")

        companion object {
            private const val SNIPPET_KEY_PREFIX = "snippet_"
        }
    }
