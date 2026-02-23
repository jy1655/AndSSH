package com.opencode.sshterminal.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.sshterminal.data.SettingsRepository
import com.opencode.sshterminal.ui.theme.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val languageTag: String = "",
    val themePreset: ThemePreset = ThemePreset.GREEN,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                settingsRepository.languageTag,
                settingsRepository.themePresetId,
            ) { lang, themeId ->
                SettingsUiState(
                    languageTag = lang,
                    themePreset = ThemePreset.fromId(themeId),
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
                SettingsUiState(),
            )

        fun setLanguageTag(tag: String) {
            viewModelScope.launch {
                settingsRepository.setLanguageTag(tag)
                val locales =
                    if (tag.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(tag)
                    }
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }

        fun setThemePreset(preset: ThemePreset) {
            viewModelScope.launch {
                settingsRepository.setThemePreset(preset.id)
            }
        }

        companion object {
            private const val STATE_FLOW_TIMEOUT_MS = 5_000L
        }
    }
