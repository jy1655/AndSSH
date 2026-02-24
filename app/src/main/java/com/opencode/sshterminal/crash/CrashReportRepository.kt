package com.opencode.sshterminal.crash

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CrashReport(
    val fileName: String,
    val timestamp: Long,
    val summary: String,
)

@Singleton
class CrashReportRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun listReports(): List<CrashReport> {
            val files =
                reportsDir()
                    .listFiles { file -> file.isFile && file.extension == "txt" }
                    ?.toList()
                    .orEmpty()

            return files
                .map { file ->
                    val content = runCatching { file.readText() }.getOrNull().orEmpty()
                    CrashReport(
                        fileName = file.name,
                        timestamp = parseTimestamp(file),
                        summary = extractSummary(content),
                    )
                }.sortedByDescending { it.timestamp }
        }

        fun readReport(fileName: String): String? {
            val file = File(reportsDir(), fileName)
            if (!file.exists() || !file.isFile) return null
            return runCatching { file.readText() }.getOrNull()
        }

        fun deleteReport(fileName: String) {
            File(reportsDir(), fileName).delete()
        }

        fun deleteAllReports() {
            reportsDir().listFiles()?.forEach { it.delete() }
        }

        fun getReportUri(fileName: String): Uri {
            val file = File(reportsDir(), fileName)
            require(file.exists() && file.isFile) { "Crash report not found: $fileName" }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        fun getReportCount(): Int {
            return reportsDir().listFiles { file -> file.isFile && file.extension == "txt" }?.size ?: 0
        }

        private fun parseTimestamp(file: File): Long {
            val value = file.nameWithoutExtension.removePrefix("crash_")
            return value.toLongOrNull() ?: file.lastModified()
        }

        private fun extractSummary(content: String): String {
            val lines = content.lineSequence().toList()
            val stackStart = lines.indexOfFirst { it.trim() == "Stacktrace:" }
            if (stackStart >= 0) {
                val firstStackLine = lines.drop(stackStart + 1).firstOrNull { it.isNotBlank() }
                if (!firstStackLine.isNullOrBlank()) return firstStackLine.trim()
            }
            return lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Unknown crash"
        }

        private fun reportsDir(): File {
            return File(context.filesDir, REPORTS_DIR).apply { mkdirs() }
        }

        private companion object {
            private const val REPORTS_DIR = "crash_reports"
        }
    }
