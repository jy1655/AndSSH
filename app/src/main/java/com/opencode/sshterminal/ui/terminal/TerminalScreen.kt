package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.sshterminal.session.SessionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateToSftp: (connectionId: String) -> Unit,
    onAllTabsClosed: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeSnapshot by viewModel.activeSnapshot.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var hadTabs by remember { mutableStateOf(false) }
    var showConnectionPicker by remember { mutableStateOf(false) }

    LaunchedEffect(tabs) {
        if (tabs.isNotEmpty()) hadTabs = true
        if (hadTabs && tabs.isEmpty()) onAllTabsClosed()
    }

    val activeTab = tabs.find { it.tabId == activeTabId }
    val connectionInfo = activeSnapshot?.let { snap ->
        if (snap.host.isNotBlank()) "${snap.username}@${snap.host}:${snap.port}" else ""
    } ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                connectionInfo = connectionInfo,
                onTerminal = { },
                onSftp = {
                    activeTab?.connectionId?.let { onNavigateToSftp(it) }
                },
                onDisconnect = { viewModel.disconnectActiveTab() }
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
            val selectedIndex = tabs.indexOfFirst { it.tabId == activeTabId }.coerceAtLeast(0)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (tabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier.weight(1f),
                        edgePadding = 4.dp,
                        divider = { }
                    ) {
                        tabs.forEachIndexed { _, tabInfo ->
                            Tab(
                                selected = tabInfo.tabId == activeTabId,
                                onClick = { viewModel.switchTab(tabInfo.tabId) }
                            ) {
                                Text(
                                    text = tabInfo.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (tabInfo.state) {
                                        SessionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                        SessionState.FAILED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .height(36.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = { showConnectionPicker = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New tab",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (activeTabId != null) {
                    IconButton(onClick = { activeTabId?.let { viewModel.closeTab(it) } }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close active tab",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (activeSnapshot?.state == SessionState.CONNECTED) {
                    IconButton(onClick = { viewModel.disconnectActiveTab() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            TerminalRenderer(
                bridge = viewModel.bridge,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onTap = { },
                onResize = { cols, rows -> viewModel.resize(cols, rows) }
            )

            val error = activeSnapshot?.error
            if (error != null) {
                Text(
                    text = "Error: $error",
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

    if (showConnectionPicker) {
        ModalBottomSheet(
            onDismissRequest = { showConnectionPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Text(
                text = "Select Connection",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            if (profiles.isEmpty()) {
                Text(
                    text = "No saved connections",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(profiles, key = { it.id }) { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showConnectionPicker = false
                                    viewModel.openTab(profile.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${profile.username}@${profile.host}:${profile.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val hostKeyAlert = activeSnapshot?.hostKeyAlert
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
