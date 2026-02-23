package com.opencode.sshterminal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val languageTag: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[LANGUAGE_TAG_KEY] ?: DEFAULT_LANGUAGE_TAG
            }

        val themePresetId: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[THEME_PRESET_KEY] ?: DEFAULT_THEME_PRESET
            }

        suspend fun setLanguageTag(tag: String) {
            dataStore.edit { prefs -> prefs[LANGUAGE_TAG_KEY] = tag }
        }

        suspend fun setThemePreset(presetId: String) {
            dataStore.edit { prefs -> prefs[THEME_PRESET_KEY] = presetId }
        }

        companion object {
            private val LANGUAGE_TAG_KEY = stringPreferencesKey("pref_language_tag")
            private val THEME_PRESET_KEY = stringPreferencesKey("pref_theme_preset")
            const val DEFAULT_LANGUAGE_TAG = ""
            const val DEFAULT_THEME_PRESET = "green"
        }
    }
