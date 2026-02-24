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
class TerminalCommandHistoryRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val encryptionManager: EncryptionManager,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        val commandHistory: Flow<List<TerminalCommandHistoryEntry>> =
            dataStore.data.map { prefs ->
                decodeHistory(prefs[TERMINAL_COMMAND_HISTORY_KEY])
                    .sortedByDescending { entry -> entry.usedAtEpochMillis }
            }

        suspend fun record(command: String) {
            val normalized = command.trimEnd('\r', '\n')
            if (normalized.isBlank()) return
            dataStore.edit { prefs ->
                val existing = decodeHistory(prefs[TERMINAL_COMMAND_HISTORY_KEY])
                val deduped = existing.filterNot { entry -> entry.command == normalized }
                val updated =
                    listOf(TerminalCommandHistoryEntry(command = normalized)) +
                        deduped
                prefs[TERMINAL_COMMAND_HISTORY_KEY] =
                    encodeHistory(updated.take(MAX_HISTORY_ITEMS))
            }
        }

        suspend fun delete(command: String) {
            dataStore.edit { prefs ->
                val existing = decodeHistory(prefs[TERMINAL_COMMAND_HISTORY_KEY])
                val updated = existing.filterNot { entry -> entry.command == command }
                if (updated.isEmpty()) {
                    prefs.remove(TERMINAL_COMMAND_HISTORY_KEY)
                } else {
                    prefs[TERMINAL_COMMAND_HISTORY_KEY] = encodeHistory(updated)
                }
            }
        }

        suspend fun clear() {
            dataStore.edit { prefs ->
                prefs.remove(TERMINAL_COMMAND_HISTORY_KEY)
            }
        }

        private fun decodeHistory(raw: String?): List<TerminalCommandHistoryEntry> {
            if (raw.isNullOrEmpty()) return emptyList()
            return runCatching {
                val decrypted = encryptionManager.decrypt(raw)
                json.decodeFromString<List<TerminalCommandHistoryEntry>>(decrypted)
            }.getOrElse { emptyList() }
        }

        private fun encodeHistory(entries: List<TerminalCommandHistoryEntry>): String {
            return encryptionManager.encrypt(json.encodeToString(entries))
        }

        companion object {
            private val TERMINAL_COMMAND_HISTORY_KEY = stringPreferencesKey("terminal_command_history")
            private const val MAX_HISTORY_ITEMS = 200
        }
    }
