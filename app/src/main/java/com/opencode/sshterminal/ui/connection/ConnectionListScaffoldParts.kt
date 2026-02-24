package com.opencode.sshterminal.ui.connection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.opencode.sshterminal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionListTopBar(
    onImportSshConfig: () -> Unit,
    onQuickConnect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.connection_list_title)) },
        actions = {
            TextButton(onClick = onImportSshConfig) {
                Text(stringResource(R.string.connection_import_ssh_config))
            }
            IconButton(onClick = onQuickConnect) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = stringResource(R.string.quick_connect_title),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.connection_settings),
                )
            }
        },
    )
}

@Composable
internal fun ConnectionListAddButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.connection_add),
        )
    }
}
