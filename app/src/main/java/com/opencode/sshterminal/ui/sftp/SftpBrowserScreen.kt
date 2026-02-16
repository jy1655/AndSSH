package com.opencode.sshterminal.ui.sftp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpBrowserScreen(
    onBack: () -> Unit,
    viewModel: SftpBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SFTP Browser") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.remotePath,
                    onValueChange = { viewModel.setRemotePath(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Remote Path") }
                )
                Button(onClick = { viewModel.list() }) { Text("List") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.downloadBasePath,
                onValueChange = { viewModel.setDownloadBasePath(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Download Base Path") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.uploadLocalPath,
                    onValueChange = { viewModel.setUploadLocalPath(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Local Upload Path") }
                )
                OutlinedTextField(
                    value = state.uploadRemotePath,
                    onValueChange = { viewModel.setUploadRemotePath(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Remote Upload Path") }
                )
            }

            Button(
                onClick = { viewModel.upload(state.uploadLocalPath, state.uploadRemotePath) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Upload")
            }

            if (state.status.isNotBlank()) {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.entries, key = { it.path }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (entry.isDirectory) "[D] ${entry.name}" else "[F] ${entry.name}",
                            modifier = Modifier.weight(1f)
                        )
                        if (entry.isDirectory) {
                            Button(onClick = { viewModel.navigateTo(entry.path) }) { Text("Open") }
                        } else {
                            Button(onClick = {
                                val basePath = state.downloadBasePath.trimEnd('/')
                                viewModel.download(entry.path, "$basePath/${entry.name}")
                            }) { Text("Download") }
                        }
                    }
                }
            }
        }
    }
}
