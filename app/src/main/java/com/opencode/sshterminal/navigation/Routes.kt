package com.opencode.sshterminal.navigation

object Routes {
    const val CONNECTION_LIST = "connection_list"
    const val TERMINAL = "terminal/{connectionId}"
    const val OPENCODE = "opencode/{connectionId}"
    const val SFTP = "sftp/{connectionId}"

    fun terminal(connectionId: String) = "terminal/$connectionId"
    fun opencode(connectionId: String) = "opencode/$connectionId"
    fun sftp(connectionId: String) = "sftp/$connectionId"
}
