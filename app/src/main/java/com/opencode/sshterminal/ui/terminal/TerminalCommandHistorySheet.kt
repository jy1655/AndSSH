package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencode.sshterminal.R
import com.opencode.sshterminal.data.TerminalCommandHistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TerminalCommandHistorySheet(
    history: List<TerminalCommandHistoryEntry>,
    onDismiss: () -> Unit,
    onRunCommand: (String) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.terminal_history_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = onClearHistory,
                    enabled = history.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.terminal_history_clear))
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.terminal_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(history, key = { entry -> entry.command }) { entry ->
                        TerminalCommandHistoryCard(
                            entry = entry,
                            onRun = { onRunCommand(entry.command) },
                            onDelete = { onDeleteCommand(entry.command) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TerminalCommandHistoryCard(
    entry: TerminalCommandHistoryEntry,
    onRun: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.command,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onRun) {
                    Text(stringResource(R.string.terminal_snippet_run))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.connection_delete))
                }
            }
        }
    }
}
