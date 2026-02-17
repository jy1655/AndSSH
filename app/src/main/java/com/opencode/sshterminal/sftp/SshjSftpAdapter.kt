package com.opencode.sshterminal.sftp

import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.HostKeyPolicy
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.util.EnumSet

class SshjSftpAdapter : SftpChannelAdapter {
    private var ssh: SSHClient? = null
    private var sftp: SFTPClient? = null

    override val isConnected: Boolean get() = ssh?.isConnected == true && sftp != null

    override suspend fun connect(request: ConnectRequest) = withContext(Dispatchers.IO) {
        close()
        val client = SSHClient()
        configureHostKeyVerifier(client, request)
        client.connect(request.host, request.port)
        authenticate(client, request)
        ssh = client
        sftp = client.newSFTPClient()
    }

    override fun close() {
        runCatching { sftp?.close() }
        runCatching { ssh?.disconnect() }
        runCatching { ssh?.close() }
        sftp = null
        ssh = null
    }

    override suspend fun list(remotePath: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        requireSftp().ls(remotePath).map { it.toRemoteEntry() }
    }

    override suspend fun exists(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            requireSftp().stat(remotePath)
            true
        } catch (t: Throwable) {
            if (isSftpNoSuchFileError(t)) false else throw t
        }
    }

    override suspend fun upload(localPath: String, remotePath: String) = withContext(Dispatchers.IO) {
        requireSftp().put(localPath, remotePath)
    }

    override suspend fun uploadStream(
        input: InputStream, remotePath: String, totalBytes: Long,
        onProgress: ((Long, Long) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val handle = requireSftp().open(remotePath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))
        try {
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            var offset = 0L
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                handle.write(offset, buf, 0, read)
                offset += read
                onProgress?.invoke(offset, totalBytes)
            }
        } finally {
            runCatching { handle.close() }
        }
    }

    override suspend fun download(remotePath: String, localPath: String) = withContext(Dispatchers.IO) {
        requireSftp().get(remotePath, localPath)
    }

    override suspend fun downloadStream(
        remotePath: String, output: OutputStream,
        onProgress: ((Long, Long) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val attrs = requireSftp().stat(remotePath)
        val totalBytes = attrs.size
        val handle = requireSftp().open(remotePath, EnumSet.of(OpenMode.READ))
        try {
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            var offset = 0L
            while (true) {
                val read = handle.read(offset, buf, 0, buf.size)
                if (read < 0) break
                output.write(buf, 0, read)
                offset += read
                onProgress?.invoke(offset, totalBytes)
            }
            output.flush()
        } finally {
            runCatching { handle.close() }
        }
    }

    override suspend fun mkdir(remotePath: String) = withContext(Dispatchers.IO) {
        requireSftp().mkdir(remotePath)
    }

    override suspend fun rm(remotePath: String) = withContext(Dispatchers.IO) {
        val attrs = requireSftp().stat(remotePath)
        if (attrs.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY) {
            requireSftp().rmdir(remotePath)
        } else {
            requireSftp().rm(remotePath)
        }
    }

    override suspend fun rename(oldPath: String, newPath: String) = withContext(Dispatchers.IO) {
        requireSftp().rename(oldPath, newPath)
    }

    private fun requireSftp(): SFTPClient =
        sftp ?: error("Not connected. Call connect() first.")

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
    override fun hostKeyUnverifiableAction(hostname: String?, key: java.security.PublicKey?): Boolean = true

    override fun hostKeyChangedAction(hostname: String?, key: java.security.PublicKey?): Boolean = true
}

internal fun isSftpNoSuchFileError(t: Throwable): Boolean {
    var cur: Throwable? = t
    while (cur != null) {
        val message = cur.message.orEmpty()
        if (
            "SSH_FX_NO_SUCH_FILE" in message ||
            "NO_SUCH_FILE" in message ||
            "No such file" in message
        ) {
            return true
        }
        cur = cur.cause
    }
    return false
}
