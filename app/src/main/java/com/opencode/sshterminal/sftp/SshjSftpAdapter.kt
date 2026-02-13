package com.opencode.sshterminal.sftp

import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.HostKeyPolicy
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class SshjSftpAdapter : SftpChannelAdapter {
    override suspend fun list(request: ConnectRequest, remotePath: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        withClient(request) { ssh ->
            ssh.newSFTPClient().use { sftp ->
                sftp.ls(remotePath).map { it.toRemoteEntry() }
            }
        }
    }

    override suspend fun upload(request: ConnectRequest, localPath: String, remotePath: String) = withContext(Dispatchers.IO) {
        withClient(request) { ssh ->
            ssh.newSFTPClient().use { sftp ->
                sftp.put(localPath, remotePath)
            }
        }
    }

    override suspend fun download(request: ConnectRequest, remotePath: String, localPath: String) = withContext(Dispatchers.IO) {
        withClient(request) { ssh ->
            ssh.newSFTPClient().use { sftp ->
                sftp.get(remotePath, localPath)
            }
        }
    }

    private inline fun <T> withClient(request: ConnectRequest, block: (SSHClient) -> T): T {
        val ssh = SSHClient()
        configureHostKeyVerifier(ssh, request)
        ssh.connect(request.host, request.port)

        authenticate(ssh, request)

        return try {
            block(ssh)
        } finally {
            runCatching { ssh.disconnect() }
            runCatching { ssh.close() }
        }
    }

    private fun authenticate(ssh: SSHClient, request: ConnectRequest) {
        when {
            !request.password.isNullOrEmpty() -> ssh.authPassword(request.username, request.password)
            !request.privateKeyPath.isNullOrEmpty() -> {
                val keyProvider = if (request.privateKeyPassphrase.isNullOrEmpty()) {
                    ssh.loadKeys(request.privateKeyPath)
                } else {
                    ssh.loadKeys(request.privateKeyPath, request.privateKeyPassphrase)
                }
                ssh.authPublickey(request.username, keyProvider)
            }
            else -> error("Either password or privateKeyPath must be provided")
        }
    }

    private fun configureHostKeyVerifier(ssh: SSHClient, request: ConnectRequest) {
        when (request.hostKeyPolicy) {
            HostKeyPolicy.TRUST_ONCE -> ssh.addHostKeyVerifier(PromiscuousVerifier())
            HostKeyPolicy.STRICT -> ssh.addHostKeyVerifier(RejectingKnownHostsVerifier(ensureFileExists(request.knownHostsPath)))
            HostKeyPolicy.UPDATE_KNOWN_HOSTS -> ssh.addHostKeyVerifier(UpdatingKnownHostsVerifier(ensureFileExists(request.knownHostsPath)))
        }
    }

    private fun ensureFileExists(path: String): File {
        val file = File(path)
        file.parentFile?.mkdirs()
        if (!file.exists()) file.createNewFile()
        return file
    }
}

private fun RemoteResourceInfo.toRemoteEntry(): RemoteEntry {
    val attributes = attributes
    return RemoteEntry(
        name = name,
        path = path,
        isDirectory = isDirectory,
        sizeBytes = attributes.size,
        modifiedEpochSec = attributes.mtime
    )
}

private class RejectingKnownHostsVerifier(knownHosts: File) : OpenSSHKnownHosts(knownHosts) {
    override fun hostKeyChangedAction(hostname: String?, key: java.security.PublicKey?): Boolean = false
}

private class UpdatingKnownHostsVerifier(knownHosts: File) : OpenSSHKnownHosts(knownHosts) {
    override fun hostKeyChangedAction(hostname: String?, key: java.security.PublicKey?): Boolean = true
}
