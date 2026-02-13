package com.opencode.sshterminal.ssh

import com.opencode.sshterminal.session.ConnectRequest

interface SshClient {
    suspend fun connect(request: ConnectRequest): SshSession
}
