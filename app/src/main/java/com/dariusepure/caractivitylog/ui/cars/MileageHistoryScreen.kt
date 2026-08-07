package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.Color
import com.dariusepure.caractivitylog.ui.common.PdfSaveDialog
import com.dariusepure.caractivitylog.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.domain.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddMileageDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var editingMileageLog by remember { mutableStateOf<MileageLog?>(null) }
    var logToDelete by remember { mutableStateOf<MileageLog?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let { destUri ->
                val successState = state as? CarDetailsUiState.Success ?: return@let
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                PdfReportGenerator.generateReport(
                                    context = context,
                                    car = successState.car,
                                    mileageLogs = successState.mileageLogs,
                                    inspections = successState.inspections,
                                    fuelLogs = successState.fuelLogs,
                                    tireSets = successState.tireSets,
                                    maintenanceLogs = successState.maintenanceLogs,
                                    outputStream = os,
                                    reportType = PdfReportGenerator.ReportType.MILEAGE_HISTORY
                                )
                            }
                        }
                        snackbarHostState.showSnackbar(context.getString(R.string.car_report_success))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar(context.getString(R.string.car_report_failed))
                    }
                }
            }
        }
    )

    if (showPdfDialog) {
        val carName = (state as? CarDetailsUiState.Success)?.car?.let { "${it.make}_${it.model}" } ?: "Car"
        PdfSaveDialog(
            onDismiss = { showPdfDialog = false },
            onSave = {
                showPdfDialog = false
                pdfLauncher.launch("Mileage_History_$carName.pdf")
            }
        )
    }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    if (logToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteMileage(carId, logToDelete!!.id)
                logToDelete = null
            },
            onDismiss = { logToDelete = null }
        )
    }

    if (showAddMileageDialog || editingMileageLog != null) {
        val successState = state as? CarDetailsUiState.Success
        val existingLogs = successState?.mileageLogs ?: emptyList()
        val car = successState?.car
        val country = europeanCountries.find { it.code == car?.plateCountry }
        val unitLabel = if (country?.usesMiles == true) "mi" else "km"

        AddMileageDialog(
            existingLog = editingMileageLog,
            existingLogs = existingLogs,
            unit = unitLabel,
            accentColor = Color(0xFF2196F3),
            onAccentColor = Color.White,
            onDismiss = { 
                showAddMileageDialog = false
                editingMileageLog = null
            },
            onConfirm = { value, date ->
                val usesMiles = country?.usesMiles == true
                val canonicalValue = CarFormatters.toCanonicalDistance(value, usesMiles)
                
                val logToEdit = editingMileageLog
                if (logToEdit != null) {
                    viewModel.updateMileage(carId, logToEdit.copy(km = canonicalValue, date = date))
                } else {
                    viewModel.addMileage(carId, canonicalValue, date)
                }
                showAddMileageDialog = false
                editingMileageLog = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mileage_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showPdfDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.car_generate_report)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMileageDialog = true },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.mileage_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val car = s.car
                val country = europeanCountries.find { it.code == car.plateCountry }
                val unitLabel = if (country?.usesMiles == true) "mi" else "km"
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = car.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))

                        if (s.mileageLogs.size >= 2) {
                            val chartData = s.mileageLogs.map { 
                                val displayKm = CarFormatters.fromCanonicalDistance(it.km, country?.usesMiles == true)
                                it.date to displayKm 
                            }
                            MileageLineChart(
                                data = chartData,
                                unit = unitLabel,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    if (s.mileageLogs.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.mileage_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(s.mileageLogs) { log ->
                            val displayValue = CarFormatters.fromCanonicalDistance(log.km, country?.usesMiles == true)
                            MileageItem(
                                log = log.copy(km = displayValue),
                                unit = unitLabel,
                                onEditClick = { editingMileageLog = log },
                                onDeleteClick = { logToDelete = log }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

