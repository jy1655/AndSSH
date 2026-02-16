package com.opencode.sshterminal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    val profiles: Flow<List<ConnectionProfile>> = dataStore.data.map { prefs ->
        prefs.asMap()
            .filter { (key, _) -> key.name.startsWith(KEY_PREFIX) }
            .values
            .mapNotNull { value ->
                runCatching { json.decodeFromString<ConnectionProfile>(value as String) }.getOrNull()
            }
            .sortedByDescending { it.lastUsedEpochMillis }
    }

    suspend fun save(profile: ConnectionProfile) {
        dataStore.edit { prefs ->
            prefs[keyFor(profile.id)] = json.encodeToString(profile)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            prefs.remove(keyFor(id))
        }
    }

    suspend fun get(id: String): ConnectionProfile? {
        val prefs = dataStore.data.first()
        val raw = prefs[keyFor(id)] ?: return null
        return runCatching { json.decodeFromString<ConnectionProfile>(raw) }.getOrNull()
    }

    suspend fun touchLastUsed(id: String) {
        dataStore.edit { prefs ->
            val raw = prefs[keyFor(id)] ?: return@edit
            val profile = runCatching { json.decodeFromString<ConnectionProfile>(raw) }.getOrNull() ?: return@edit
            prefs[keyFor(id)] = json.encodeToString(
                profile.copy(lastUsedEpochMillis = System.currentTimeMillis())
            )
        }
    }

    private fun keyFor(id: String) = stringPreferencesKey("$KEY_PREFIX$id")

    companion object {
        private const val KEY_PREFIX = "conn_"
    }
}
