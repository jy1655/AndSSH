package com.opencode.sshterminal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.opencode.sshterminal.navigation.SSHNavHost
import com.opencode.sshterminal.service.SshForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, SshForegroundService::class.java))

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                SSHNavHost(navController = navController)
            }
        }
    }
}
