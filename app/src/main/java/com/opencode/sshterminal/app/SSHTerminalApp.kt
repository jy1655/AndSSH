package com.opencode.sshterminal.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.opencode.sshterminal.auth.AutoLockManager
import com.opencode.sshterminal.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class SSHTerminalApp : Application() {
    @Inject
    lateinit var autoLockManager: AutoLockManager

    override fun onCreate() {
        super.onCreate()
        installBouncyCastleProvider()
        installCrashHandler()
        ProcessLifecycleOwner.get().lifecycle.addObserver(autoLockManager)
    }

    private fun installBouncyCastleProvider() {
        val current = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (current == null || current.javaClass != BouncyCastleProvider::class.java) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this, defaultHandler))
    }
}
