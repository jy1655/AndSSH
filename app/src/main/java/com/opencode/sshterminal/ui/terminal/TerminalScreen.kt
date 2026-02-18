package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.sshterminal.session.SessionState
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    onNavigateToSftp: (connectionId: String) -> Unit,
    onDisconnected: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsState()
    var previousState by remember { mutableStateOf(snapshot.state) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(snapshot.state) {
        if (previousState == SessionState.CONNECTED && snapshot.state == SessionState.DISCONNECTED) {
            onDisconnected()
        }
        previousState = snapshot.state
    }

    val connectionInfo = if (snapshot.host.isNotBlank()) {
        "${snapshot.username}@${snapshot.host}:${snapshot.port}"
    } else ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                connectionInfo = connectionInfo,
                onTerminal = { },
                onSftp = { onNavigateToSftp(viewModel.connectionId) },
                onDisconnect = { viewModel.disconnect() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            TerminalRenderer(
                bridge = viewModel.bridge,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onTap = { },
                onResize = { cols, rows -> viewModel.resize(cols, rows) }
            )

            if (snapshot.error != null) {
                Text(
                    text = "Error: ${snapshot.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            TerminalInputBar(
                onSendBytes = { bytes -> viewModel.sendInput(bytes) },
                onMenuClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.fillMaxWidth()
            )
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

