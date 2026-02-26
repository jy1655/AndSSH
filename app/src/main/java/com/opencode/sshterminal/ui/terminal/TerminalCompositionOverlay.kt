package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
internal fun TerminalCompositionOverlay(
    composingText: String,
    cursorOffsetX: Float,
    cursorOffsetY: Float,
    charHeight: Float,
    terminalSize: IntSize,
) {
    if (composingText.isEmpty() || terminalSize.width <= 0) {
        return
    }

    val popupPositionProvider =
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val cursorX = anchorBounds.left + cursorOffsetX.roundToInt()
                val cursorY = anchorBounds.top + cursorOffsetY.roundToInt()
                val verticalGap = 4
                val cursorHeight = charHeight.roundToInt().coerceAtLeast(0)
                val shouldFlipAbove = cursorOffsetY > (terminalSize.height * (2f / 3f))

                val rawX = if (layoutDirection == LayoutDirection.Rtl) cursorX - popupContentSize.width else cursorX
                val rawY =
                    if (shouldFlipAbove) {
                        cursorY - popupContentSize.height - verticalGap
                    } else {
                        cursorY + cursorHeight + verticalGap
                    }

                val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)

                return IntOffset(
                    x = rawX.coerceIn(0, maxX),
                    y = rawY.coerceIn(0, maxY),
                )
            }
        }

    val backgroundColor = MaterialTheme.colorScheme.inverseSurface
    val contentColor = MaterialTheme.colorScheme.inverseOnSurface

    Popup(
        popupPositionProvider = popupPositionProvider,
        properties =
            PopupProperties(
                focusable = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 4.dp,
            color = backgroundColor,
            contentColor = contentColor,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = composingText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.9f)),
                )
            }
        }
    }
}
