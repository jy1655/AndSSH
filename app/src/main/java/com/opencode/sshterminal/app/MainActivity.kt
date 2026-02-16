package com.opencode.sshterminal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.opencode.sshterminal.navigation.SSHNavHost
import com.opencode.sshterminal.service.SshForegroundService
import com.opencode.sshterminal.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, SshForegroundService::class.java))

        setContent {
            AppTheme {
                val navController = rememberNavController()
                SSHNavHost(navController = navController)
            }
        }
    }
}
