package com.dariusepure.caractivitylog.ui.cars

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.CarReport
import com.dariusepure.caractivitylog.ui.common.EmptyState
import com.dariusepure.caractivitylog.ui.common.PdfPreviewDialog
import com.dariusepure.caractivitylog.ui.common.toRelativeString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarReportsScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: CarReportsViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var reportToDelete by remember { mutableStateOf<CarReport?>(null) }
    var exportingReport by remember { mutableStateOf<CarReport?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let { destUri ->
                exportingReport?.let { report ->
                    try {
                        val sourceFile = File(context.filesDir, "reports/${report.fileName}")
                        context.contentResolver.openOutputStream(destUri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.car_report_success), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.car_report_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            exportingReport = null
        }
    )

    LaunchedEffect(carId) {
        viewModel.loadReports(carId)
    }

    if (reportToDelete != null) {
        com.dariusepure.caractivitylog.ui.common.DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteReport(carId, reportToDelete!!)
                reportToDelete = null
            },
            onDismiss = { reportToDelete = null },
            message = stringResource(R.string.car_reports_delete_confirm)
        )
    }

    if (previewFile != null) {
        PdfPreviewDialog(
            pdfFile = previewFile!!,
            onDismiss = { previewFile = null },
            onSave = {
                val report = reports.find { File(context.filesDir, "reports/${it.fileName}").absolutePath == previewFile!!.absolutePath }
                if (report != null) {
                    exportingReport = report
                    exportLauncher.launch(report.fileName)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isGenerating) viewModel.generateReport(carId) },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.car_reports_generate_new))
                }
            }
        }
    ) { padding ->
        if (reports.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.car_reports_empty),
                subtitle = "",
                icon = Icons.Default.Description,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportItem(
                        report = report,
                        onView = {
                            val file = File(context.filesDir, "reports/${report.fileName}")
                            if (file.exists()) {
                                previewFile = file
                            }
                        },
                        onShare = {
                            val file = File(context.filesDir, "reports/${report.fileName}")
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.car_reports_share)))
                            }
                        },
                        onExport = {
                            exportingReport = report
                            exportLauncher.launch(report.fileName)
                        },
                        onDelete = { reportToDelete = report }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportItem(
    report: CarReport,
    onView: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateStr = report.date.toRelativeString(context)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.car_reports_export),
                    tint = Color(0xFF1A73E8)
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.car_reports_view)) },
                        leadingIcon = { Icon(Icons.Default.Visibility, null) },
                        onClick = {
                            expanded = false
                            onView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.car_reports_share)) },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            expanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.car_reports_export)) },
                        leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                        onClick = {
                            expanded = false
                            onExport()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

