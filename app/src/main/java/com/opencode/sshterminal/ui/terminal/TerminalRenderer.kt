package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.opencode.sshterminal.terminal.TermuxTerminalBridge
import com.termux.terminal.TerminalBuffer
import com.termux.terminal.TextStyle as TermuxTextStyle

private val TERMINAL_FONT_SIZE = 12.sp

private val ANSI_COLORS = arrayOf(
    Color(0xFF000000), // 0 black
    Color(0xFFCD0000), // 1 red
    Color(0xFF00CD00), // 2 green
    Color(0xFFCDCD00), // 3 yellow
    Color(0xFF0000EE), // 4 blue
    Color(0xFFCD00CD), // 5 magenta
    Color(0xFF00CDCD), // 6 cyan
    Color(0xFFE5E5E5), // 7 white
    Color(0xFF7F7F7F), // 8 bright black
    Color(0xFFFF0000), // 9 bright red
    Color(0xFF00FF00), // 10 bright green
    Color(0xFFFFFF00), // 11 bright yellow
    Color(0xFF5C5CFF), // 12 bright blue
    Color(0xFFFF00FF), // 13 bright magenta
    Color(0xFF00FFFF), // 14 bright cyan
    Color(0xFFFFFFFF)  // 15 bright white
)

private val DEFAULT_FG = Color(0xFFCCCCCC)
private val DEFAULT_BG = Color(0xFF1E1E1E)
private val CURSOR_COLOR = Color(0xFFA0A0A0)

@Composable
fun TerminalRenderer(
    bridge: TermuxTerminalBridge,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val renderVersion by bridge.renderVersion.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    val charSize = remember(textMeasurer) {
        val result = textMeasurer.measure(
            "W",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = TERMINAL_FONT_SIZE)
        )
        Size(result.size.width.toFloat(), result.size.height.toFloat())
    }

    @Suppress("UNUSED_EXPRESSION")
    renderVersion

    Canvas(
        modifier = modifier
            .focusable()
            .pointerInput(Unit) {
                if (onTap != null) {
                    detectTapGestures { onTap() }
                }
            }
    ) {
        drawRect(DEFAULT_BG, Offset.Zero, size)

        val screen = bridge.screen
        val rows = bridge.termRows
        val cols = bridge.termCols

        for (row in 0 until rows) {
            drawTerminalRow(screen, row, cols, charSize, textMeasurer)
        }

        val cx = bridge.cursorCol * charSize.width
        val cy = bridge.cursorRow * charSize.height
        if (bridge.emulator.shouldCursorBeVisible()) {
            drawRect(CURSOR_COLOR, Offset(cx, cy), Size(charSize.width, charSize.height), alpha = 0.5f)
        }
    }
}

private fun DrawScope.drawTerminalRow(
    screen: TerminalBuffer,
    row: Int,
    cols: Int,
    charSize: Size,
    textMeasurer: TextMeasurer
) {
    val termRow = screen.allocateFullLineIfNecessary(screen.externalToInternalRow(row))
    val y = row * charSize.height

    var col = 0
    while (col < cols) {
        val style = termRow.getStyle(col)
        val effect = TermuxTextStyle.decodeEffect(style)
        val fgIndex = TermuxTextStyle.decodeForeColor(style)
        val bgIndex = TermuxTextStyle.decodeBackColor(style)

        val inverse = (effect and com.termux.terminal.TextStyle.CHARACTER_ATTRIBUTE_INVERSE) != 0
        var fg = resolveColor(fgIndex, DEFAULT_FG)
        var bg = resolveColor(bgIndex, DEFAULT_BG)
        if (inverse) {
            val tmp = fg; fg = bg; bg = tmp
        }

        val x = col * charSize.width
        val startCol = termRow.findStartOfColumn(col)
        val codePoint = if (startCol >= 0 && startCol < termRow.spaceUsed) {
            Character.codePointAt(termRow.mText, startCol)
        } else {
            ' '.code
        }
        val ch = if (codePoint in 0x20..0x10FFFF) {
            String(Character.toChars(codePoint))
        } else {
            " "
        }

        if (bg != DEFAULT_BG) {
            drawRect(bg, Offset(x, y), charSize)
        }

        if (ch.isNotBlank()) {
            val bold = (effect and com.termux.terminal.TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0
            drawText(
                textMeasurer,
                ch,
                topLeft = Offset(x, y),
                style = TextStyle(
                    color = fg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TERMINAL_FONT_SIZE,
                    fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null
                )
            )
        }
        col++
    }
}

private fun resolveColor(index: Int, default: Color): Color {
    return when {
        index == TermuxTextStyle.COLOR_INDEX_FOREGROUND -> DEFAULT_FG
        index == TermuxTextStyle.COLOR_INDEX_BACKGROUND -> DEFAULT_BG
        index in 0..15 -> ANSI_COLORS[index]
        index in 16..255 -> color256(index)
        else -> default
    }
}

private fun color256(index: Int): Color {
    if (index < 16) return ANSI_COLORS[index]
    if (index < 232) {
        val i = index - 16
        val r = (i / 36) * 51
        val g = ((i % 36) / 6) * 51
        val b = (i % 6) * 51
        return Color(r, g, b)
    }
    val gray = 8 + (index - 232) * 10
    return Color(gray, gray, gray)
}
