package com.opencode.sshterminal.ui.terminal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opencode.sshterminal.R
import com.opencode.sshterminal.session.SessionState
import com.opencode.sshterminal.session.TabInfo

@Composable
internal fun TerminalTabLabel(tabInfo: TabInfo) {
    val spec = resolveTabLabelSpec(tabInfo.state, MaterialTheme.colorScheme)

    Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tabInfo.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = spec.titleColor,
        )
        Text(
            text = stringResource(spec.statusResId),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = spec.statusColor,
        )
    }
}

private data class TabLabelSpec(
    val titleColor: Color,
    @StringRes val statusResId: Int,
    val statusColor: Color,
)

private fun resolveTabLabelSpec(
    state: SessionState,
    colorScheme: ColorScheme,
): TabLabelSpec =
    when (state) {
        SessionState.CONNECTED ->
            TabLabelSpec(
                titleColor = colorScheme.onSurface,
                statusResId = R.string.terminal_tab_status_connected,
                statusColor = colorScheme.primary,
            )
        SessionState.CONNECTING ->
            TabLabelSpec(
                titleColor = colorScheme.onSurface,
                statusResId = R.string.terminal_tab_status_connecting,
                statusColor = colorScheme.primary,
            )
        SessionState.RECONNECTING ->
            TabLabelSpec(
                titleColor = colorScheme.onSurface,
                statusResId = R.string.terminal_tab_status_reconnecting,
                statusColor = colorScheme.primary,
            )
        SessionState.FAILED ->
            TabLabelSpec(
                titleColor = colorScheme.error,
                statusResId = R.string.terminal_tab_status_failed,
                statusColor = colorScheme.error,
            )
        SessionState.DISCONNECTED ->
            TabLabelSpec(
                titleColor = colorScheme.onSurfaceVariant,
                statusResId = R.string.terminal_tab_status_disconnected,
                statusColor = colorScheme.onSurfaceVariant,
            )
        SessionState.IDLE ->
            TabLabelSpec(
                titleColor = colorScheme.onSurfaceVariant,
                statusResId = R.string.terminal_tab_status_idle,
                statusColor = colorScheme.onSurfaceVariant,
            )
    }
