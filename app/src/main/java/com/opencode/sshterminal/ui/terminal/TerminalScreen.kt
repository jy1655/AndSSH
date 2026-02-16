package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.sshterminal.session.SessionState

@Composable
fun TerminalScreen(
    onNavigateToOpenCode: (connectionId: String) -> Unit,
    onNavigateToSftp: (connectionId: String) -> Unit,
    onDisconnected: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsState()
    var stdinText by remember { mutableStateOf("") }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var previousState by remember { mutableStateOf(snapshot.state) }

    LaunchedEffect(snapshot.state) {
        if (previousState == SessionState.CONNECTED && snapshot.state == SessionState.DISCONNECTED) {
            onDisconnected()
        }
        previousState = snapshot.state
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        TerminalRenderer(
            bridge = viewModel.bridge,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onTap = { }
        )

        if (snapshot.error != null) {
            Text(
                text = "Error: ${snapshot.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { onNavigateToOpenCode(viewModel.connectionId) },
                modifier = Modifier.height(36.dp)
            ) {
                Text("OpenCode")
            }
            Button(
                onClick = { onNavigateToSftp(viewModel.connectionId) },
                modifier = Modifier.height(36.dp)
            ) {
                Text("SFTP")
            }
            Button(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.height(36.dp)
            ) {
                Text("Disconnect")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { ctrlArmed = !ctrlArmed }, modifier = Modifier.height(36.dp)) {
                Text(if (ctrlArmed) "Ctrl*" else "Ctrl")
            }
            Button(onClick = { altArmed = !altArmed }, modifier = Modifier.height(36.dp)) {
                Text(if (altArmed) "Alt*" else "Alt")
            }
            Button(onClick = { viewModel.sendInput(byteArrayOf(0x1B)); ctrlArmed = false; altArmed = false }, modifier = Modifier.height(36.dp)) {
                Text("ESC")
            }
            Button(onClick = { viewModel.sendText("\t"); ctrlArmed = false; altArmed = false }, modifier = Modifier.height(36.dp)) {
                Text("TAB")
            }
            Button(onClick = { viewModel.sendText("\r"); ctrlArmed = false; altArmed = false }, modifier = Modifier.height(36.dp)) {
                Text("ENTER")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { viewModel.sendText("\u001B[A") }, modifier = Modifier.height(36.dp)) { Text("↑") }
            Button(onClick = { viewModel.sendText("\u001B[B") }, modifier = Modifier.height(36.dp)) { Text("↓") }
            Button(onClick = { viewModel.sendText("\u001B[D") }, modifier = Modifier.height(36.dp)) { Text("←") }
            Button(onClick = { viewModel.sendText("\u001B[C") }, modifier = Modifier.height(36.dp)) { Text("→") }
            Button(onClick = { viewModel.sendInput(byteArrayOf(0x03)) }, modifier = Modifier.height(36.dp)) { Text("^C") }
            Button(onClick = { viewModel.sendInput(byteArrayOf(0x04)) }, modifier = Modifier.height(36.dp)) { Text("^D") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = stdinText,
                onValueChange = { stdinText = it },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                placeholder = { Text("Input...") }
            )
            Button(
                onClick = {
                    val payload = buildInputPayload(stdinText, ctrlArmed, altArmed)
                    if (payload.isNotEmpty()) {
                        viewModel.sendInput(payload)
                        stdinText = ""
                        ctrlArmed = false
                        altArmed = false
                    }
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Send")
            }
        }
    }

    val hostKeyAlert = snapshot.hostKeyAlert
    if (hostKeyAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHostKeyAlert() },
            title = { Text("Host Key Changed") },
            text = {
                Text(
                    "Host: ${hostKeyAlert.host}:${hostKeyAlert.port}\n" +
                        "Fingerprint: ${hostKeyAlert.fingerprint}"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissHostKeyAlert() }) {
                    Text("Reject")
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { viewModel.trustHostKeyOnce() }) { Text("Trust Once") }
                    TextButton(onClick = { viewModel.updateKnownHostsAndReconnect() }) { Text("Update known_hosts") }
                }
            }
        )
    }
}

private fun buildInputPayload(text: String, ctrlArmed: Boolean, altArmed: Boolean): ByteArray {
    if (text.isEmpty() && !ctrlArmed && !altArmed) return ByteArray(0)
    val core = when {
        ctrlArmed && text.length == 1 -> {
            val upper = text[0].uppercaseChar().code
            byteArrayOf(if (upper in 64..95) (upper - 64).toByte() else text[0].code.toByte())
        }
        else -> text.toByteArray(Charsets.UTF_8)
    }
    return if (altArmed) byteArrayOf(0x1B) + core else core
}
