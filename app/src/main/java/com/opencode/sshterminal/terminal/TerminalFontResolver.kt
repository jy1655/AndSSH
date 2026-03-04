package com.opencode.sshterminal.terminal

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.FontRes
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.opencode.sshterminal.R

private val HANGUL_FALLBACK_FONT_RES_ID = R.font.nanum_gothic_coding_regular

internal fun resolveTerminalTypeface(
    context: Context,
    terminalFontId: String,
): Typeface {
    val primaryFontResId = TerminalFontPreset.fromId(terminalFontId).fontResId

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            return buildTerminalTypefaceWithFallback(
                context = context,
                primaryFontResId = primaryFontResId,
                fallbackFontResId = HANGUL_FALLBACK_FONT_RES_ID,
            )
        }
    }

    return ResourcesCompat.getFont(context, primaryFontResId) ?: Typeface.MONOSPACE
}

internal fun resolveTerminalComposeFontFamily(terminalFontId: String): FontFamily {
    val primaryFontResId = TerminalFontPreset.fromId(terminalFontId).fontResId
    return FontFamily(
        Font(primaryFontResId),
        Font(HANGUL_FALLBACK_FONT_RES_ID),
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun buildTerminalTypefaceWithFallback(
    context: Context,
    @FontRes primaryFontResId: Int,
    @FontRes fallbackFontResId: Int,
): Typeface {
    val primaryFamily =
        android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(context.resources, primaryFontResId).build(),
        ).build()
    val fallbackFamily =
        android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(context.resources, fallbackFontResId).build(),
        ).build()
    return Typeface
        .CustomFallbackBuilder(primaryFamily)
        .addCustomFallback(fallbackFamily)
        .build()
}
