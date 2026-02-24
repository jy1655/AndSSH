package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencode.sshterminal.R
import com.opencode.sshterminal.data.TerminalSnippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TerminalSnippetSheet(
    snippets: List<TerminalSnippet>,
    onDismiss: () -> Unit,
    onSaveSnippet: (existingId: String?, title: String, command: String) -> Unit,
    onRunSnippet: (TerminalSnippet) -> Unit,
    onDeleteSnippet: (TerminalSnippet) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editingSnippetId by rememberSaveable { mutableStateOf<String?>(null) }
    var snippetTitle by rememberSaveable { mutableStateOf("") }
    var snippetCommand by rememberSaveable { mutableStateOf("") }
    val filteredSnippets =
        remember(snippets, searchQuery) {
            filterTerminalSnippets(snippets = snippets, searchQuery = searchQuery)
        }
    val isEditing = editingSnippetId != null

    fun resetEditor() {
        editingSnippetId = null
        snippetTitle = ""
        snippetCommand = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.terminal_snippets_title),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.terminal_snippet_search_label)) },
                placeholder = { Text(stringResource(R.string.terminal_snippet_search_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = snippetTitle,
                onValueChange = { snippetTitle = it },
                label = { Text(stringResource(R.string.terminal_snippet_title_label)) },
                placeholder = { Text(stringResource(R.string.terminal_snippet_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = snippetCommand,
                onValueChange = { snippetCommand = it },
                label = { Text(stringResource(R.string.terminal_snippet_command_label)) },
                placeholder = { Text(stringResource(R.string.terminal_snippet_command_placeholder)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = ::resetEditor) {
                    Text(
                        stringResource(
                            if (isEditing) {
                                R.string.terminal_snippet_cancel_edit
                            } else {
                                R.string.terminal_snippet_clear_input
                            },
                        ),
                    )
                }
                Button(
                    onClick = {
                        onSaveSnippet(editingSnippetId, snippetTitle, snippetCommand)
                        resetEditor()
                    },
                    enabled = snippetCommand.isNotBlank(),
                ) {
                    Text(
                        stringResource(
                            if (isEditing) {
                                R.string.terminal_snippet_update
                            } else {
                                R.string.terminal_snippet_save
                            },
                        ),
                    )
                }
            }

            if (filteredSnippets.isEmpty()) {
                Text(
                    text =
                        if (searchQuery.trim().isBlank()) {
                            stringResource(R.string.terminal_snippet_empty)
                        } else {
                            stringResource(R.string.terminal_snippet_empty_search, searchQuery.trim())
                        },
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
                    items(filteredSnippets, key = { snippet -> snippet.id }) { snippet ->
                        TerminalSnippetCard(
                            snippet = snippet,
                            onRun = { onRunSnippet(snippet) },
                            onEdit = {
                                editingSnippetId = snippet.id
                                snippetTitle = snippet.title
                                snippetCommand = snippet.command
                            },
                            onDelete = { onDeleteSnippet(snippet) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TerminalSnippetCard(
    snippet: TerminalSnippet,
    onRun: () -> Unit,
    onEdit: () -> Unit,
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
                text = snippet.title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = snippet.command,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onRun) {
                    Text(stringResource(R.string.terminal_snippet_run))
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.connection_edit))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.connection_delete))
                }
            }
        }
    }
}
