package com.opencode.sshterminal.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.sshterminal.BuildConfig
import com.opencode.sshterminal.R
import com.opencode.sshterminal.ui.theme.ClassicPurple
import com.opencode.sshterminal.ui.theme.OceanBlue
import com.opencode.sshterminal.ui.theme.SunsetOrange
import com.opencode.sshterminal.ui.theme.TerminalGreen
import com.opencode.sshterminal.ui.theme.ThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.sftp_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            LanguageSection(
                selected = state.languageTag,
                onSelect = viewModel::setLanguageTag,
            )
            Spacer(modifier = Modifier.height(24.dp))
            ThemeSection(
                selected = state.themePreset,
                onSelect = viewModel::setThemePreset,
            )
            Spacer(modifier = Modifier.height(24.dp))
            AboutSection()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun LanguageSection(
    selected: String,
    onSelect: (String) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_language_title))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            LanguageOption(
                label = stringResource(R.string.settings_language_system),
                tag = "",
                selected = selected,
                onSelect = onSelect,
            )
            LanguageOption(
                label = stringResource(R.string.settings_language_english),
                tag = "en",
                selected = selected,
                onSelect = onSelect,
            )
            LanguageOption(
                label = stringResource(R.string.settings_language_korean),
                tag = "ko",
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    tag: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val isSelected = selected == tag
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(tag) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ThemeSection(
    selected: ThemePreset,
    onSelect: (ThemePreset) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_theme_title))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ThemeCircle(
            preset = ThemePreset.GREEN,
            color = TerminalGreen,
            label = stringResource(R.string.settings_theme_green),
            isSelected = selected == ThemePreset.GREEN,
            onSelect = onSelect,
        )
        ThemeCircle(
            preset = ThemePreset.OCEAN,
            color = OceanBlue,
            label = stringResource(R.string.settings_theme_ocean),
            isSelected = selected == ThemePreset.OCEAN,
            onSelect = onSelect,
        )
        ThemeCircle(
            preset = ThemePreset.SUNSET,
            color = SunsetOrange,
            label = stringResource(R.string.settings_theme_sunset),
            isSelected = selected == ThemePreset.SUNSET,
            onSelect = onSelect,
        )
        ThemeCircle(
            preset = ThemePreset.PURPLE,
            color = ClassicPurple,
            label = stringResource(R.string.settings_theme_purple),
            isSelected = selected == ThemePreset.PURPLE,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ThemeCircle(
    preset: ThemePreset,
    color: Color,
    label: String,
    isSelected: Boolean,
    onSelect: (ThemePreset) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onSelect(preset) },
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            border =
                if (isSelected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                } else {
                    null
                },
            modifier = Modifier.size(48.dp),
        ) {
            if (isSelected) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutSection() {
    SectionHeader(stringResource(R.string.settings_about_title))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        stringResource(
                            R.string.settings_about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
