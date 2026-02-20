package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sshterminal.R
import com.opencode.sshterminal.terminal.TermuxTerminalBridge
import com.opencode.sshterminal.ui.theme.TerminalBackground
import com.opencode.sshterminal.ui.theme.TerminalCursor
import com.opencode.sshterminal.ui.theme.TerminalForeground
import com.termux.terminal.TerminalBuffer
import com.termux.terminal.WcWidth
import com.termux.terminal.TextStyle as TermuxTextStyle

private val TERMINAL_FONT_SIZE = 12.sp
private val TERMINAL_FONT_FAMILY = FontFamily(Font(R.font.meslo_lgs_nf_regular))

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

private val DEFAULT_FG = TerminalForeground
private val DEFAULT_BG = TerminalBackground
private val CURSOR_COLOR = TerminalCursor

data class TerminalSelection(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
) {
    val normalized: TerminalSelection
        get() {
            return if (startRow < endRow || (startRow == endRow && startCol <= endCol)) {
                this
            } else {
                TerminalSelection(endRow, endCol, startRow, startCol)
            }
        }

    val isEmpty: Boolean
        get() = startRow == endRow && startCol == endCol
}

@Composable
fun TerminalRenderer(
    bridge: TermuxTerminalBridge,
    modifier: Modifier = Modifier,
    pageUpCount: Int = 0,
    pageDownCount: Int = 0,
    onTap: (() -> Unit)? = null,
    onResize: ((cols: Int, rows: Int) -> Unit)? = null,
    onCopyText: ((String) -> Unit)? = null
) {
    val renderVersion by bridge.renderVersion.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    val charSize = remember(textMeasurer) {
        val result = textMeasurer.measure(
            "W",
            style = TextStyle(fontFamily = TERMINAL_FONT_FAMILY, fontSize = TERMINAL_FONT_SIZE)
        )
        Size(result.size.width.toFloat(), result.size.height.toFloat())
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scrollOffset by remember(bridge) { mutableStateOf(0) }
    var scrollPixelAccumulator by remember(bridge) { mutableStateOf(0f) }
    var selection by remember { mutableStateOf<TerminalSelection?>(null) }
    val charHeightPx by rememberUpdatedState(charSize.height)
    val charWidthPx by rememberUpdatedState(charSize.width)

    LaunchedEffect(canvasSize, charSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && charSize.width > 0 && charSize.height > 0) {
            val cols = (canvasSize.width / charSize.width).toInt().coerceAtLeast(1)
            val rows = (canvasSize.height / charSize.height).toInt().coerceAtLeast(1)
            if (cols != bridge.termCols || rows != bridge.termRows) {
                onResize?.invoke(cols, rows)
            }
        }
    }

    // Auto-scroll: only snap to bottom when user is already at bottom
    LaunchedEffect(renderVersion) {
        if (scrollOffset == 0) {
            scrollPixelAccumulator = 0f
        }
    }

    LaunchedEffect(pageUpCount) {
        if (pageUpCount > 0) {
            val pageRows = bridge.termRows.coerceAtLeast(1)
            val maxScroll = bridge.screen.activeTranscriptRows
            scrollOffset = (scrollOffset + pageRows).coerceIn(0, maxScroll)
        }
    }

    LaunchedEffect(pageDownCount) {
        if (pageDownCount > 0) {
            scrollOffset = (scrollOffset - bridge.termRows.coerceAtLeast(1)).coerceAtLeast(0)
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    renderVersion

    Box(
        modifier = modifier
            .focusable()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    val touchSlop = viewConfiguration.touchSlop
                    val longPressAt = down.uptimeMillis + longPressTimeout
                    val downPosition = down.position
                    var pointerId = down.id
                    var lastPosition = down.position
                    var selectionMode = false
                    var scrollMode = false
                    var ended = false

                    while (!ended) {
                        val event = awaitPointerEvent()
                        val primaryChange = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                        if (primaryChange == null) {
                            break
                        }
                        val change = primaryChange
                        pointerId = change.id

                        if (change.changedToUpIgnoreConsumed()) {
                            ended = true
                            break
                        }

                        if (!selectionMode && !scrollMode && change.uptimeMillis >= longPressAt) {
                            val (bufferRow, col) = offsetToCell(
                                offset = downPosition,
                                charWidthPx = charWidthPx,
                                charHeightPx = charHeightPx,
                                scrollOffset = scrollOffset,
                                cols = bridge.termCols,
                                rows = bridge.termRows
                            )
                            selection = TerminalSelection(
                                startRow = bufferRow,
                                startCol = col,
                                endRow = bufferRow,
                                endCol = col
                            )
                            selectionMode = true
                        }

                        if (!selectionMode && !scrollMode) {
                            val totalDelta = change.position - downPosition
                            if (totalDelta.getDistance() > touchSlop) {
                                scrollMode = true
                            }
                        }

                        if (selectionMode) {
                            val (bufferRow, col) = offsetToCell(
                                offset = change.position,
                                charWidthPx = charWidthPx,
                                charHeightPx = charHeightPx,
                                scrollOffset = scrollOffset,
                                cols = bridge.termCols,
                                rows = bridge.termRows
                            )
                            selection = selection?.copy(
                                endRow = bufferRow,
                                endCol = (col + 1).coerceAtMost(bridge.termCols)
                            )
                            change.consume()
                        } else if (scrollMode) {
                            val dragAmount = change.position.y - lastPosition.y
                            if (charHeightPx > 0f) {
                                val maxScroll = bridge.screen.activeTranscriptRows
                                val result = computeScrollUpdate(
                                    dragAmount, scrollOffset,
                                    scrollPixelAccumulator, charHeightPx, maxScroll
                                )
                                scrollOffset = result.newScrollOffset
                                scrollPixelAccumulator = result.newPixelAccumulator
                            }
                            change.consume()
                        }

                        lastPosition = change.position
                    }

                    if (scrollMode) {
                        scrollPixelAccumulator = 0f
                    }

                    if (!selectionMode && !scrollMode && ended) {
                        if (selection != null) {
                            selection = null
                        } else {
                            onTap?.invoke()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(DEFAULT_BG, Offset.Zero, size)

            bridge.withReadLock {
                val screen = bridge.screen
                val rows = bridge.termRows
                val cols = bridge.termCols
                val effectiveScroll = scrollOffset.coerceIn(0, screen.activeTranscriptRows)

                for (screenRow in 0 until rows) {
                    val bufferRow = screenRow - effectiveScroll
                    drawTerminalRow(screen, bufferRow, screenRow, cols, charSize, textMeasurer)
                }

                val cursorScreenRow = bridge.cursorRow + effectiveScroll
                if (cursorScreenRow in 0 until rows && bridge.emulator.shouldCursorBeVisible()) {
                    val cx = bridge.cursorCol * charSize.width
                    val cy = cursorScreenRow * charSize.height
                    drawRect(CURSOR_COLOR, Offset(cx, cy), Size(charSize.width, charSize.height), alpha = 0.5f)
                }

                selection?.normalized?.let { sel ->
                    for (screenRow in 0 until rows) {
                        val bufferRow = screenRow - effectiveScroll
                        if (bufferRow < sel.startRow || bufferRow > sel.endRow) continue

                        val startCol = if (bufferRow == sel.startRow) sel.startCol else 0
                        val endCol = if (bufferRow == sel.endRow) sel.endCol else cols
                        if (endCol <= startCol) continue

                        val x = startCol * charSize.width
                        val w = (endCol - startCol) * charSize.width
                        val y = screenRow * charSize.height
                        drawRect(
                            color = Color(0x5033B5E5),
                            topLeft = Offset(x, y),
                            size = Size(w, charSize.height)
                        )
                    }
                }
            }
        }

        if (selection != null && selection?.isEmpty == false && onCopyText != null) {
            Button(
                onClick = {
                    bridge.withReadLock {
                        val text = extractSelectedText(bridge.screen, selection!!, bridge.termCols)
                        if (text.isNotEmpty()) onCopyText(text)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text("Copy")
            }
        }
    }
}

private fun offsetToCell(
    offset: Offset,
    charWidthPx: Float,
    charHeightPx: Float,
    scrollOffset: Int,
    cols: Int,
    rows: Int
): Pair<Int, Int> {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val safeCharWidth = charWidthPx.coerceAtLeast(1f)
    val safeCharHeight = charHeightPx.coerceAtLeast(1f)
    val screenRow = (offset.y / safeCharHeight).toInt().coerceIn(0, safeRows - 1)
    val col = (offset.x / safeCharWidth).toInt().coerceIn(0, safeCols - 1)
    val bufferRow = screenRow - scrollOffset
    return Pair(bufferRow, col)
}

private fun extractSelectedText(screen: TerminalBuffer, selection: TerminalSelection, cols: Int): String {
    val sel = selection.normalized
    val safeCols = cols.coerceAtLeast(1)
    val sb = StringBuilder()
    for (row in sel.startRow..sel.endRow) {
        val internalRow = screen.externalToInternalRow(row)
        val termRow = screen.allocateFullLineIfNecessary(internalRow)
        val colStart = if (row == sel.startRow) sel.startCol else 0
        val colEnd = if (row == sel.endRow) sel.endCol else safeCols
        var col = colStart
        while (col < colEnd) {
            val startOfCol = termRow.findStartOfColumn(col)
            val codePoint = if (startOfCol >= 0 && startOfCol < termRow.spaceUsed) {
                Character.codePointAt(termRow.mText, startOfCol)
            } else {
                ' '.code
            }
            if (codePoint in 0x20..0x10FFFF) {
                sb.appendCodePoint(codePoint)
            }
            val width = if (codePoint > 0x7F) WcWidth.width(codePoint).coerceAtLeast(1) else 1
            col += width
        }
        if (row < sel.endRow) sb.append('\n')
    }
    return sb.toString()
}

private fun DrawScope.drawTerminalRow(
    screen: TerminalBuffer,
    bufferRow: Int,
    screenRow: Int,
    cols: Int,
    charSize: Size,
    textMeasurer: TextMeasurer
) {
    val termRow = screen.allocateFullLineIfNecessary(screen.externalToInternalRow(bufferRow))
    val y = screenRow * charSize.height
    if (y >= size.height) return

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
        if (x >= size.width) break
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
        val charWidth = if (codePoint > 0x7F) WcWidth.width(codePoint).coerceAtLeast(1) else 1

        if (bg != DEFAULT_BG) {
            drawRect(bg, Offset(x, y), Size(charSize.width * charWidth, charSize.height))
        }

        if (ch.isNotBlank()) {
            val bold = (effect and com.termux.terminal.TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0
            val safeX = x.coerceAtMost((size.width - 1f).coerceAtLeast(0f))
            drawText(
                textMeasurer,
                ch,
                topLeft = Offset(safeX, y),
                style = TextStyle(
                    color = fg,
                    fontFamily = TERMINAL_FONT_FAMILY,
                    fontSize = TERMINAL_FONT_SIZE,
                    fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null
                )
            )
        }
        col += charWidth
    }
}

private fun resolveColor(index: Int, default: Color): Color {
    return when {
        index == TermuxTextStyle.COLOR_INDEX_FOREGROUND -> DEFAULT_FG
        index == TermuxTextStyle.COLOR_INDEX_BACKGROUND -> DEFAULT_BG
        (index and 0xFF000000.toInt()) == 0xFF000000.toInt() -> argbToColor(index)
        index in 0..15 -> ANSI_COLORS[index]
        index in 16..255 -> color256(index)
        else -> default
    }
}

private fun argbToColor(argb: Int): Color {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return Color(r, g, b)
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

/**
 * Pure scroll-offset calculation extracted from the gesture handler.
 *
 * Convention:
 *  - scrollOffset 0 = live (bottom) view; >0 = scrolled back into history.
 *  - Swipe UP   → dragAmount < 0 → rowDelta < 0 → (−rowDelta) > 0 → offset increases  (history).
 *  - Swipe DOWN → dragAmount > 0 → rowDelta > 0 → (−rowDelta) < 0 → offset decreases (toward live).
 */
internal data class ScrollUpdate(
    val newScrollOffset: Int,
    val newPixelAccumulator: Float
)

internal fun computeScrollUpdate(
    dragAmount: Float,
    currentScrollOffset: Int,
    pixelAccumulator: Float,
    charHeightPx: Float,
    maxScroll: Int
): ScrollUpdate {
    val newAccumulator = pixelAccumulator + dragAmount
    val rowDelta = (newAccumulator / charHeightPx).toInt()
    return if (rowDelta != 0) {
        ScrollUpdate(
            newScrollOffset = (currentScrollOffset - rowDelta).coerceIn(0, maxScroll),
            newPixelAccumulator = newAccumulator - rowDelta * charHeightPx
        )
    } else {
        ScrollUpdate(
            newScrollOffset = currentScrollOffset,
            newPixelAccumulator = newAccumulator
        )
    }
}
