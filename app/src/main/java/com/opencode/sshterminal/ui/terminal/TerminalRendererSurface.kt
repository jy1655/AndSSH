package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencode.sshterminal.R
import com.opencode.sshterminal.terminal.TermuxTerminalBridge

@Composable
internal fun TerminalRenderSurface(
    bridge: TermuxTerminalBridge,
    modifier: Modifier,
    state: TerminalRenderState,
    config: TerminalRenderConfig,
) {
    Box(modifier = modifier.focusable()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                bridge.withReadLock {
                    val emulator = bridge.emulator
                    val effectiveScroll = state.scrollOffset.coerceIn(0, bridge.screen.activeTranscriptRows)
                    val topRow = -effectiveScroll

                    val sel = state.selection?.normalized
                    val selY1 = sel?.startRow?.plus(effectiveScroll) ?: -1
                    val selY2 = sel?.endRow?.plus(effectiveScroll) ?: -1
                    val selX1 = sel?.startCol ?: -1
                    val selX2 = sel?.endCol ?: -1

                    config.termuxRenderer.render(
                        emulator,
                        canvas.nativeCanvas,
                        topRow,
                        selY1,
                        selY2,
                        selX1,
                        selX2,
                    )
                }
            }
        }
        val selection = state.selection
        val onCopyText = config.onCopyText
        if (selection != null && !selection.isEmpty && onCopyText != null) {
            Button(
                onClick = {
                    bridge.withReadLock {
                        val text = extractSelectedText(bridge.screen, selection, bridge.termCols)
                        if (text.isNotEmpty()) {
                            onCopyText(text)
                        }
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
            ) {
                Text(stringResource(R.string.terminal_copy))
            }
        }
    }
}

internal data class TerminalRenderState(
    val scrollOffset: Int,
    val selection: TerminalSelection?,
)

internal data class TerminalRenderConfig(
    val charSize: Size,
    val termuxRenderer: com.termux.view.TerminalRenderer,
    val onCopyText: ((String) -> Unit)?,
)
