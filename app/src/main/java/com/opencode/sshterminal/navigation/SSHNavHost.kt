package com.opencode.sshterminal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.opencode.sshterminal.ui.connection.ConnectionListScreen
import com.opencode.sshterminal.ui.opencode.OpenCodeScreen
import com.opencode.sshterminal.ui.sftp.SftpBrowserScreen
import com.opencode.sshterminal.ui.terminal.TerminalScreen

@Composable
fun SSHNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CONNECTION_LIST,
        modifier = modifier
    ) {
        composable(Routes.CONNECTION_LIST) {
            ConnectionListScreen(
                onConnect = { connectionId ->
                    navController.navigate(Routes.terminal(connectionId))
                }
            )
        }

        composable(
            route = Routes.TERMINAL,
            arguments = listOf(navArgument("connectionId") { type = NavType.StringType })
        ) {
            TerminalScreen(
                onNavigateToOpenCode = { connectionId ->
                    navController.navigate(Routes.opencode(connectionId))
                },
                onNavigateToSftp = { connectionId ->
                    navController.navigate(Routes.sftp(connectionId))
                },
                onDisconnected = {
                    navController.popBackStack(Routes.CONNECTION_LIST, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.OPENCODE,
            arguments = listOf(navArgument("connectionId") { type = NavType.StringType })
        ) {
            OpenCodeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SFTP,
            arguments = listOf(navArgument("connectionId") { type = NavType.StringType })
        ) {
            SftpBrowserScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
