package com.opencode.sshterminal.app

import android.content.Intent
import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.opencode.sshterminal.service.SshForegroundService
import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.SessionStateMachine
import com.opencode.sshterminal.sftp.RemoteEntry
import com.opencode.sshterminal.sftp.SshjSftpAdapter
import com.opencode.sshterminal.ssh.SshjClient
import com.opencode.sshterminal.terminal.SimpleTerminalEngine
import kotlinx.coroutines.launch
import kotlin.text.Charsets.UTF_8

class MainActivity : ComponentActivity() {
    private val terminalEngine = SimpleTerminalEngine()
    private val sessionStateMachine = SessionStateMachine(SshjClient(), terminalEngine)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, SshForegroundService::class.java))

        setContent {
            MaterialTheme {
                TerminalScreen(sessionStateMachine, terminalEngine)
            }
        }
    }

    override fun onDestroy() {
        sessionStateMachine.destroy()
        super.onDestroy()
    }
}

@Composable
private fun TerminalScreen(
    sessionStateMachine: SessionStateMachine,
    terminalEngine: SimpleTerminalEngine
) {
    val snapshot by sessionStateMachine.snapshot.collectAsState()
    val screen by terminalEngine.screen.collectAsState()
    val sftpAdapter = remember { SshjSftpAdapter() }
    val scope = rememberCoroutineScope()

    var host by remember { mutableStateOf("example.com") }
    var username by remember { mutableStateOf("user") }
    var password by remember { mutableStateOf("") }
    var knownHostsPath by remember { mutableStateOf("/data/data/com.opencode.sshterminal/files/known_hosts") }
    var privateKeyPath by remember { mutableStateOf("/sdcard/.ssh/id_ed25519") }
    var stdinText by remember { mutableStateOf("") }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var remotePath by remember { mutableStateOf(".") }
    var localUploadPath by remember { mutableStateOf("") }
    var remoteUploadPath by remember { mutableStateOf("") }
    var localDownloadPath by remember { mutableStateOf("") }
    var sftpEntries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var sftpStatus by remember { mutableStateOf("") }
    var sftpBusy by remember { mutableStateOf(false) }
    var pendingOverwrite by remember { mutableStateOf<OverwriteAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("State: ${snapshot.state}")

        LabeledField(label = "Host", value = host, onValueChange = { host = it })
        LabeledField(label = "Username", value = username, onValueChange = { username = it })
        LabeledField(
            label = "Password (optional)",
            value = password,
            onValueChange = { password = it },
            isPassword = true
        )
        LabeledField(label = "known_hosts Path", value = knownHostsPath, onValueChange = { knownHostsPath = it })
        LabeledField(
            label = "Private Key Path (optional)",
            value = privateKeyPath,
            onValueChange = { privateKeyPath = it }
        )

        Button(onClick = {
            sessionStateMachine.connect(
                ConnectRequest(
                    host = host,
                    username = username,
                    knownHostsPath = knownHostsPath,
                    password = password.ifBlank { null },
                    privateKeyPath = privateKeyPath.ifBlank { null },
                    cols = 120,
                    rows = 40
                )
            )
        }) {
            Text("Connect")
        }

        Button(onClick = { sessionStateMachine.disconnect() }) {
            Text("Disconnect")
        }

        Text("Terminal")
        Text(
            text = screen.ifBlank { "(no output)" },
            modifier = Modifier
                .fillMaxWidth()
        )

        if (snapshot.error != null) {
            Text("Error: ${snapshot.error}")
        }

        LabeledField(
            label = "stdin",
            value = stdinText,
            onValueChange = { stdinText = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(onClick = { ctrlArmed = !ctrlArmed }) { Text(if (ctrlArmed) "Ctrl*" else "Ctrl") }
            Button(onClick = { altArmed = !altArmed }) { Text(if (altArmed) "Alt*" else "Alt") }
            Button(onClick = { sessionStateMachine.sendInput(byteArrayOf(0x1B.toByte())); ctrlArmed = false; altArmed = false }) { Text("ESC") }
            Button(onClick = { sessionStateMachine.sendInput("\t".toByteArray()); ctrlArmed = false; altArmed = false }) { Text("TAB") }
            Button(onClick = { sessionStateMachine.sendInput("\r".toByteArray()); ctrlArmed = false; altArmed = false }) { Text("ENTER") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(onClick = { sessionStateMachine.sendInput("\u001B[A".toByteArray()) }) { Text("UP") }
            Button(onClick = { sessionStateMachine.sendInput("\u001B[B".toByteArray()) }) { Text("DOWN") }
            Button(onClick = { sessionStateMachine.sendInput("\u001B[D".toByteArray()) }) { Text("LEFT") }
            Button(onClick = { sessionStateMachine.sendInput("\u001B[C".toByteArray()) }) { Text("RIGHT") }
            Button(onClick = { sessionStateMachine.sendInput(byteArrayOf(0x03.toByte())) }) { Text("^C") }
            Button(onClick = { sessionStateMachine.sendInput(byteArrayOf(0x04.toByte())) }) { Text("^D") }
        }
        Button(onClick = {
            val payload = buildInputPayload(stdinText, ctrlArmed, altArmed)
            if (payload.isNotEmpty()) {
                sessionStateMachine.sendInput(payload)
                stdinText = ""
                ctrlArmed = false
                altArmed = false
            }
        }) {
            Text("Send stdin")
        }

        Text("SFTP Browser")
        LabeledField(
            label = "Remote Path",
            value = remotePath,
            onValueChange = { remotePath = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(onClick = {
                val request = currentConnectRequest(
                    host = host,
                    username = username,
                    knownHostsPath = knownHostsPath,
                    password = password,
                    privateKeyPath = privateKeyPath
                )
                scope.launch {
                    runCatching {
                        sftpBusy = true
                        sftpStatus = "Listing $remotePath ..."
                        sftpEntries = sftpAdapter.list(request, remotePath.ifBlank { "." })
                        sftpStatus = "Listed ${sftpEntries.size} entries"
                    }.onFailure {
                        sftpStatus = "List failed: ${toUserFriendlySftpError(it)}"
                    }.also {
                        sftpBusy = false
                    }
                }
            }) { Text("List") }
            Button(onClick = {
                sftpEntries = emptyList()
                sftpStatus = ""
            }) { Text("Clear") }
        }

        LabeledField(
            label = "Local Upload Path",
            value = localUploadPath,
            onValueChange = { localUploadPath = it }
        )
        LabeledField(
            label = "Remote Upload Path",
            value = remoteUploadPath,
            onValueChange = { remoteUploadPath = it }
        )
        Button(onClick = {
            val request = currentConnectRequest(
                host = host,
                username = username,
                knownHostsPath = knownHostsPath,
                password = password,
                privateKeyPath = privateKeyPath
            )
            scope.launch {
                if (localUploadPath.isBlank() || remoteUploadPath.isBlank()) {
                    sftpStatus = "Upload failed: local/remote path를 모두 입력하세요."
                    return@launch
                }
                val local = File(localUploadPath)
                if (!local.exists()) {
                    sftpStatus = "Upload failed: 로컬 파일이 존재하지 않습니다."
                    return@launch
                }
                runCatching {
                    sftpBusy = true
                    val remoteExists = sftpAdapter.exists(request, remoteUploadPath)
                    if (remoteExists) {
                        pendingOverwrite = OverwriteAction.Upload(request, localUploadPath, remoteUploadPath)
                        sftpStatus = "Remote file exists. Confirm overwrite."
                    } else {
                        sftpStatus = "Uploading ..."
                        sftpAdapter.upload(request, localUploadPath, remoteUploadPath)
                        sftpStatus = "Upload completed"
                    }
                }.onFailure {
                    sftpStatus = "Upload failed: ${toUserFriendlySftpError(it)}"
                }.also {
                    sftpBusy = false
                }
            }
        }) { Text("Upload") }

        LabeledField(
            label = "Local Download Base Dir",
            value = localDownloadPath,
            onValueChange = { localDownloadPath = it }
        )

        if (sftpStatus.isNotBlank()) {
            Text("SFTP: $sftpStatus")
        }
        if (sftpBusy) {
            CircularProgressIndicator()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(sftpEntries, key = { it.path }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (entry.isDirectory) "[D] ${entry.name}" else "[F] ${entry.name}",
                        modifier = Modifier.weight(1f)
                    )
                    if (entry.isDirectory) {
                        Button(onClick = { remotePath = entry.path }) {
                            Text("Open")
                        }
                    } else {
                        Button(onClick = {
                            val request = currentConnectRequest(
                                host = host,
                                username = username,
                                knownHostsPath = knownHostsPath,
                                password = password,
                                privateKeyPath = privateKeyPath
                            )
                            val base = localDownloadPath.ifBlank { "/sdcard/Download" }
                            val target = "$base/${entry.name}"
                            scope.launch {
                                val localTarget = File(target)
                                if (localTarget.exists()) {
                                    pendingOverwrite = OverwriteAction.Download(request, entry.path, target)
                                    sftpStatus = "Local file exists. Confirm overwrite."
                                    return@launch
                                }
                                runCatching {
                                    sftpBusy = true
                                    sftpStatus = "Downloading ${entry.name} ..."
                                    sftpAdapter.download(request, entry.path, target)
                                    sftpStatus = "Downloaded to $target"
                                }.onFailure {
                                    sftpStatus = "Download failed: ${toUserFriendlySftpError(it)}"
                                }.also {
                                    sftpBusy = false
                                }
                            }
                        }) {
                            Text("Download")
                        }
                    }
                }
            }
        }
    }

    val hostKeyAlert = snapshot.hostKeyAlert
    if (hostKeyAlert != null) {
        AlertDialog(
            onDismissRequest = { sessionStateMachine.dismissHostKeyAlert() },
            title = { Text("Host Key Changed") },
            text = {
                Text(
                    "Host: ${hostKeyAlert.host}:${hostKeyAlert.port}\n" +
                        "Fingerprint: ${hostKeyAlert.fingerprint}\n\n" +
                        "known_hosts에 저장된 키와 서버 키가 다릅니다.\n" +
                        "기본 동작은 연결 거부입니다."
                )
            },
            confirmButton = {
                TextButton(onClick = { sessionStateMachine.dismissHostKeyAlert() }) {
                    Text("Reject (Default)")
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { sessionStateMachine.trustHostKeyOnce() }) {
                        Text("Trust Once")
                    }
                    TextButton(onClick = { sessionStateMachine.updateKnownHostsAndReconnect() }) {
                        Text("Update known_hosts")
                    }
                }
            }
        )
    }

    val overwrite = pendingOverwrite
    if (overwrite != null) {
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text("Overwrite confirmation") },
            text = {
                Text(
                    when (overwrite) {
                        is OverwriteAction.Upload -> "Remote file exists:\n${overwrite.remotePath}\n덮어쓸까요?"
                        is OverwriteAction.Download -> "Local file exists:\n${overwrite.localPath}\n덮어쓸까요?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            sftpBusy = true
                            when (overwrite) {
                                is OverwriteAction.Upload -> {
                                    sftpStatus = "Uploading ..."
                                    sftpAdapter.upload(overwrite.request, overwrite.localPath, overwrite.remotePath)
                                    sftpStatus = "Upload completed"
                                }
                                is OverwriteAction.Download -> {
                                    sftpStatus = "Downloading ..."
                                    sftpAdapter.download(overwrite.request, overwrite.remotePath, overwrite.localPath)
                                    sftpStatus = "Download completed"
                                }
                            }
                        }.onFailure {
                            sftpStatus = "Transfer failed: ${toUserFriendlySftpError(it)}"
                        }.also {
                            sftpBusy = false
                            pendingOverwrite = null
                        }
                    }
                }) {
                    Text("Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwrite = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun currentConnectRequest(
    host: String,
    username: String,
    knownHostsPath: String,
    password: String,
    privateKeyPath: String
): ConnectRequest {
    return ConnectRequest(
        host = host,
        username = username,
        knownHostsPath = knownHostsPath,
        password = password.ifBlank { null },
        privateKeyPath = privateKeyPath.ifBlank { null },
        cols = 120,
        rows = 40
    )
}

private fun buildInputPayload(text: String, ctrlArmed: Boolean, altArmed: Boolean): ByteArray {
    if (text.isEmpty() && !ctrlArmed && !altArmed) return ByteArray(0)

    val core = when {
        ctrlArmed && text.length == 1 -> byteArrayOf(ctrlByte(text[0]))
        else -> text.toByteArray(UTF_8)
    }

    return if (altArmed) byteArrayOf(0x1B.toByte()) + core else core
}

private fun ctrlByte(ch: Char): Byte {
    val upper = ch.uppercaseChar().code
    return if (upper in 64..95) (upper - 64).toByte() else ch.code.toByte()
}

private sealed interface OverwriteAction {
    data class Upload(
        val request: ConnectRequest,
        val localPath: String,
        val remotePath: String
    ) : OverwriteAction

    data class Download(
        val request: ConnectRequest,
        val remotePath: String,
        val localPath: String
    ) : OverwriteAction
}

private fun toUserFriendlySftpError(t: Throwable): String {
    val message = t.message ?: return "알 수 없는 오류"
    return when {
        "Permission denied" in message -> "권한이 없습니다. 원격 경로/파일 권한을 확인하세요."
        "No such file" in message -> "경로가 존재하지 않습니다. remote/local 경로를 확인하세요."
        "Auth fail" in message || "Authentication" in message -> "인증 실패. 사용자명/비밀번호 또는 키 설정을 확인하세요."
        else -> message
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
