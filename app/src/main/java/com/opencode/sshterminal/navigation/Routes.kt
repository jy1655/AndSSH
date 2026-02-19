package com.opencode.sshterminal.navigation

object Routes {
    const val CONNECTION_LIST = "connection_list"
    const val TERMINAL = "terminal"
    const val SFTP = "sftp/{connectionId}"

    fun sftp(connectionId: String) = "sftp/$connectionId"
}
