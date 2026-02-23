package com.opencode.sshterminal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private fun buildScheme(
    primary: androidx.compose.ui.graphics.Color,
    primaryDark: androidx.compose.ui.graphics.Color,
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = SurfaceBlack,
        primaryContainer = primaryDark,
        onPrimaryContainer = TextPrimary,
        secondary = primary,
        onSecondary = SurfaceBlack,
        background = SurfaceBlack,
        onBackground = TextPrimary,
        surface = SurfaceDim,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceCard,
        onSurfaceVariant = TextSecondary,
        surfaceContainerHighest = SurfaceContainer,
        error = ErrorRed,
        onError = SurfaceBlack,
    )

private val GreenScheme = buildScheme(TerminalGreen, TerminalGreenDark)
private val OceanScheme = buildScheme(OceanBlue, OceanBlueDark)
private val SunsetScheme = buildScheme(SunsetOrange, SunsetOrangeDark)
private val PurpleScheme = buildScheme(ClassicPurple, ClassicPurpleDark)

fun colorSchemeFor(preset: ThemePreset): ColorScheme =
    when (preset) {
        ThemePreset.GREEN -> GreenScheme
        ThemePreset.OCEAN -> OceanScheme
        ThemePreset.SUNSET -> SunsetScheme
        ThemePreset.PURPLE -> PurpleScheme
    }

@Composable
fun AppTheme(
    themePreset: ThemePreset = ThemePreset.GREEN,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(themePreset),
        typography = AppTypography,
        content = content,
    )
}
