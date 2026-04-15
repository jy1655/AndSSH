package com.opencode.sshterminal.e2e

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.sshterminal.app.MainActivity
import com.opencode.sshterminal.service.BellNotifier
import com.opencode.sshterminal.session.ConnectRequest
import com.opencode.sshterminal.session.HostKeyPolicy
import com.opencode.sshterminal.session.SessionManager
import com.opencode.sshterminal.session.SessionState
import com.opencode.sshterminal.session.TabId
import com.opencode.sshterminal.ssh.SshjClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class RealSshConnectionDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()

    @Test
    fun app_connectsToCurrentWorkspace_andExecutesProbeCommand() =
        runBlocking {
            val host = argumentOrNull("andsshTestSshHost")
            val port = argumentOrNull("andsshTestSshPort")?.toIntOrNull()
            val username = argumentOrNull("andsshTestSshUsername")
            val privateKeyBase64 = argumentOrNull("andsshTestPrivateKeyBase64")
            val workspacePath = argumentOrNull("andsshTestWorkspacePath")
            val probeToken = argumentOrNull("andsshTestProbeToken")
            val allArgsPresent =
                !host.isNullOrBlank() &&
                    port != null &&
                    !username.isNullOrBlank() &&
                    !privateKeyBase64.isNullOrBlank() &&
                    !workspacePath.isNullOrBlank() &&
                    !probeToken.isNullOrBlank()
            assumeTrue(
                "device SSH E2E runner args are required; run via scripts/device-ssh-e2e.sh",
                allArgsPresent,
            )

            launchMainActivity()
            val sessionManager = createSessionManager()

            val privateKeyFile = writePrivateKeyFile(requireNotNull(privateKeyBase64))
            val request =
                ConnectRequest(
                    host = requireNotNull(host),
                    port = requireNotNull(port),
                    username = requireNotNull(username),
                    knownHostsPath = File(targetContext.filesDir, "android-test-known_hosts").absolutePath,
                    privateKeyPath = privateKeyFile.absolutePath,
                    hostKeyPolicy = HostKeyPolicy.TRUST_ONCE,
                    cols = 120,
                    rows = 40,
                )

            val tabId =
                sessionManager.openTab(
                    title = "SSH E2E",
                    connectionId = "device-ssh-e2e",
                    request = request,
                )

            val connectionSnapshot =
                withTimeout(CONNECT_TIMEOUT_MS) {
                    sessionManager.activeSnapshot
                        .filterNotNull()
                        .first { snapshot ->
                            snapshot.state == SessionState.CONNECTED || snapshot.state == SessionState.FAILED
                        }
                }
            assertEquals(connectionSnapshot.error, SessionState.CONNECTED, connectionSnapshot.state)

            delay(500)
            val probeCommand =
                "cd ${shellQuote(requireNotNull(workspacePath))} && pwd && printf '${requireNotNull(probeToken)}\\n'\r"
            sessionManager.sendInputToTab(tabId, probeCommand.toByteArray(Charsets.UTF_8))

            val transcript =
                awaitTranscript(
                    sessionManager = sessionManager,
                    tabId = tabId,
                    workspacePath = requireNotNull(workspacePath),
                    probeToken = requireNotNull(probeToken),
                )
            assertTrue(transcript.contains(requireNotNull(workspacePath)))
            assertTrue(transcript.contains(requireNotNull(probeToken)))

            sessionManager.closeTab(tabId)
        }

    private fun launchMainActivity() {
        val launchIntent =
            Intent(targetContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        targetContext.startActivity(launchIntent)
        instrumentation.waitForIdleSync()
    }

    private fun closeExistingTabs(sessionManager: SessionManager) {
        sessionManager.tabs.value.forEach { tab -> sessionManager.closeTab(tab.tabId) }
    }

    private fun createSessionManager(): SessionManager {
        return SessionManager(
            sshClient = SshjClient(),
            bellNotifier = BellNotifier(targetContext),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ).also(::closeExistingTabs)
    }

    private fun writePrivateKeyFile(privateKeyBase64: String): File {
        val privateKeyFile = File(targetContext.filesDir, "android-test-real-ssh-key")
        privateKeyFile.writeBytes(Base64.getDecoder().decode(privateKeyBase64))
        privateKeyFile.setReadable(true, true)
        privateKeyFile.setWritable(true, true)
        return privateKeyFile
    }

    private suspend fun awaitTranscript(
        sessionManager: SessionManager,
        tabId: TabId,
        workspacePath: String,
        probeToken: String,
    ): String {
        return withTimeout(TRANSCRIPT_TIMEOUT_MS) {
            var transcript =
                sessionManager
                    .bridgeForTab(tabId)
                    ?.withReadLock { screen.getTranscriptText() }
                    .orEmpty()
            while (!transcript.contains(workspacePath) || !transcript.contains(probeToken)) {
                transcript =
                    sessionManager
                        .bridgeForTab(tabId)
                        ?.withReadLock { screen.getTranscriptText() }
                        .orEmpty()
                delay(TRANSCRIPT_POLL_INTERVAL_MS)
            }
            transcript
        }
    }

    private fun argumentOrNull(name: String): String? = arguments.getString(name)?.takeIf { it.isNotBlank() }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30_000L
        private const val TRANSCRIPT_TIMEOUT_MS = 30_000L
        private const val TRANSCRIPT_POLL_INTERVAL_MS = 250L
    }
}
