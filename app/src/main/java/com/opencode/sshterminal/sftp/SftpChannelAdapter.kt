package com.opencode.sshterminal.sftp

import com.opencode.sshterminal.session.ConnectRequest

interface SftpChannelAdapter {
    suspend fun list(request: ConnectRequest, remotePath: String): List<RemoteEntry>
    suspend fun upload(request: ConnectRequest, localPath: String, remotePath: String)
    suspend fun download(request: ConnectRequest, remotePath: String, localPath: String)
}
