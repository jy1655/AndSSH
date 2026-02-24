package com.opencode.sshterminal.navigation

object Routes {
    const val CONNECTION_LIST = "connection_list"
    const val TERMINAL = "terminal"
    const val SFTP = "sftp/{connectionId}"
    const val SETTINGS = "settings"
    const val CRASH_LOGS = "crash_logs"

    fun sftp(connectionId: String) = "sftp/$connectionId"
}
