package com.opencode.sshterminal.ui.sftp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.sshterminal.sftp.RemoteEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SftpBrowserScreen(
    onBack: () -> Unit,
    viewModel: SftpBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMkdirDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<RemoteEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<RemoteEntry?>(null) }
    var contextMenuEntry by remember { mutableStateOf<RemoteEntry?>(null) }
    var pendingDownloadPath by remember { mutableStateOf("") }

    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && pendingDownloadPath.isNotEmpty()) {
            viewModel.downloadToStream(pendingDownloadPath, uri)
        }
        pendingDownloadPath = ""
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val fileName = docFile?.name ?: "upload_${System.currentTimeMillis()}"
            viewModel.uploadFromUri(uri, fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SFTP Browser") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { uploadLauncher.launch("*/*") }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                    }
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showMkdirDialog = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Breadcrumbs(
                path = state.remotePath,
                onNavigate = { viewModel.navigateTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            AnimatedVisibility(
                visible = state.transferProgress >= 0f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { state.transferProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            AnimatedVisibility(
                visible = state.status.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(state.entries, key = { it.path }) { entry ->
                    Box {
                        FileEntryRow(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    viewModel.navigateTo(entry.path)
                                }
                            },
                            onLongClick = {
                                contextMenuEntry = entry
                            }
                        )

                        DropdownMenu(
                            expanded = contextMenuEntry == entry,
                            onDismissRequest = { contextMenuEntry = null }
                        ) {
                            if (entry.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text("Open") },
                                    leadingIcon = {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                                    },
                                    onClick = {
                                        contextMenuEntry = null
                                        viewModel.navigateTo(entry.path)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Download") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    },
                                    onClick = {
                                        contextMenuEntry = null
                                        pendingDownloadPath = entry.path
                                        downloadLauncher.launch(entry.name)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                                },
                                onClick = {
                                    contextMenuEntry = null
                                    renameTarget = entry
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    contextMenuEntry = null
                                    deleteTarget = entry
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMkdirDialog) {
        InputDialog(
            title = "New Folder",
            placeholder = "Folder name",
            confirmLabel = "Create",
            onConfirm = { name ->
                showMkdirDialog = false
                if (name.isNotBlank()) viewModel.mkdir(name)
            },
            onDismiss = { showMkdirDialog = false }
        )
    }

    renameTarget?.let { entry ->
        InputDialog(
            title = "Rename",
            placeholder = "New name",
            initialValue = entry.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                renameTarget = null
                if (newName.isNotBlank() && newName != entry.name) {
                    viewModel.rename(entry.path, newName)
                }
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete") },
            text = { Text("Delete \"${entry.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    viewModel.rm(entry.path)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------- Reusable input dialog ----------

@Composable
private fun InputDialog(
    title: String,
    placeholder: String,
    initialValue: String = "",
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------- Breadcrumbs ----------

@Composable
private fun Breadcrumbs(
    path: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = path.split("/").filter { it.isNotEmpty() }

    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TextButton(onClick = { onNavigate("/") }) {
                Text("/", style = MaterialTheme.typography.labelLarge)
            }
        }
        itemsIndexed(segments) { index, segment ->
            Icon(
                Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val fullPath = "/" + segments.take(index + 1).joinToString("/")
            TextButton(onClick = { onNavigate(fullPath) }) {
                Text(segment, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---------- File entry row ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileEntryRow(
    entry: RemoteEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!entry.isDirectory) {
                    Text(
                        text = formatFileSize(entry.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.modifiedEpochSec > 0) {
                    Text(
                        text = formatDate(entry.modifiedEpochSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!entry.isDirectory) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------- Utilities ----------

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun formatDate(epochSec: Long): String = dateFormat.format(Date(epochSec * 1000))
