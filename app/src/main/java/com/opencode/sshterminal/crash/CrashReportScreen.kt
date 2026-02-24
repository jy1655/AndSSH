package com.opencode.sshterminal.crash

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.opencode.sshterminal.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CrashReportUiState(
    val reports: List<CrashReport> = emptyList(),
)

@HiltViewModel
class CrashReportViewModel
    @Inject
    constructor(
        private val crashReportRepository: CrashReportRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(CrashReportUiState())
            private set

        init {
            refresh()
        }

        fun refresh() {
            uiState = CrashReportUiState(reports = crashReportRepository.listReports())
        }

        fun readReport(fileName: String): String? = crashReportRepository.readReport(fileName)

        fun deleteReport(fileName: String) {
            crashReportRepository.deleteReport(fileName)
            refresh()
        }

        fun deleteAllReports() {
            crashReportRepository.deleteAllReports()
            refresh()
        }

        fun getReportUri(fileName: String) = crashReportRepository.getReportUri(fileName)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CrashReportScreen(
    onBack: () -> Unit,
    viewModel: CrashReportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    var selectedReport by remember { mutableStateOf<CrashReport?>(null) }
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    fun share(fileName: String) {
        val uri = runCatching { viewModel.getReportUri(fileName) }.getOrNull() ?: return
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.crash_report_share)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.crash_logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.sftp_back),
                        )
                    }
                },
                actions = {
                    if (state.reports.isNotEmpty()) {
                        TextButton(onClick = viewModel::deleteAllReports) {
                            Text(stringResource(R.string.crash_logs_delete_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.crash_logs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.reports, key = { it.fileName }) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedReport = report },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        top = 12.dp,
                                        bottom = 12.dp,
                                        end = 8.dp,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatter.format(Date(report.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = report.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { share(report.fileName) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.crash_report_share),
                                )
                            }
                            IconButton(onClick = { viewModel.deleteReport(report.fileName) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.crash_report_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val report = selectedReport
    if (report != null) {
        val reportText = remember(report.fileName) { viewModel.readReport(report.fileName).orEmpty() }
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            title = { Text(report.fileName) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                ) {
                    Text(text = reportText)
                }
            },
            confirmButton = {
                TextButton(onClick = { share(report.fileName) }) {
                    Text(stringResource(R.string.crash_report_share))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.deleteReport(report.fileName)
                        selectedReport = null
                    }) {
                        Text(
                            text = stringResource(R.string.crash_report_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = { selectedReport = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            },
        )
    }
}
