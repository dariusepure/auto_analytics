package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.domain.displayName
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    var showAddMileageDialog by remember { mutableStateOf(false) }
    var showLogImportDialog by remember { mutableStateOf(false) }
    var editingMileageLog by remember { mutableStateOf<MileageLog?>(null) }
    var logToDelete by remember { mutableStateOf<MileageLog?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    if (showLogImportDialog) {
        val successState = state as? CarDetailsUiState.Success
        if (successState != null) {
            LogImportDialog(
                fuelLogs = successState.fuelLogs,
                maintenanceLogs = successState.maintenanceLogs,
                inspections = successState.inspections,
                existingMileageLogs = successState.mileageLogs,
                unit = if (europeanCountries.find { it.code == successState.car.plateCountry }?.usesMiles == true) "mi" else "km",
                onDismiss = { showLogImportDialog = false },
                onConfirm = { logsToImport ->
                    viewModel.addBatchMileageLogs(carId, logsToImport)
                    showLogImportDialog = false
                }
            )
        }
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
            accentColor = Color(0xFF1A73E8),
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mileage_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showLogImportDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Import from other logs")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMileageDialog = true },
                containerColor = Color(0xFF1A73E8),
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

@Composable
fun LogImportDialog(
    fuelLogs: List<com.dariusepure.caractivitylog.domain.FuelLog>,
    maintenanceLogs: List<com.dariusepure.caractivitylog.domain.Maintenance>,
    inspections: List<com.dariusepure.caractivitylog.domain.VehicleInspection>,
    existingMileageLogs: List<MileageLog>,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (List<MileageLog>) -> Unit
) {
    val potentialLogs = remember(fuelLogs, maintenanceLogs, inspections, existingMileageLogs) {
        val list = mutableListOf<Pair<MileageLog, String>>() // Log + Source Label
        
        val existingKms = existingMileageLogs.map { it.km.roundToInt() }.toSet()
        
        fuelLogs.forEach { fuel ->
            if (fuel.km.roundToInt() !in existingKms) {
                list.add(MileageLog(km = fuel.km, date = fuel.date) to "Fuel")
            }
        }
        
        maintenanceLogs.forEach { service ->
            if (service.km.roundToInt() !in existingKms) {
                list.add(MileageLog(km = service.km, date = service.date) to "Service")
            }
        }

        inspections.forEach { inspection ->
            if (inspection.mileage.roundToInt() !in existingKms) {
                list.add(MileageLog(km = inspection.mileage, date = inspection.date) to "Inspection")
            }
        }
        
        list.distinctBy { it.first.km.roundToInt() }.sortedByDescending { it.first.date }
    }

    var selectedLogs by remember { mutableStateOf(potentialLogs.map { it.first }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Logs") },
        text = {
            Column {
                if (potentialLogs.isEmpty()) {
                    Text("No new mileage records found in Fuel or Service logs.")
                } else {
                    Text(
                        text = "We found ${potentialLogs.size} records in other sections. Select what to add to history.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(potentialLogs) { (log, source) ->
                            val isSelected = log in selectedLogs
                            Surface(
                                onClick = {
                                    selectedLogs = if (isSelected) selectedLogs - log else selectedLogs + log
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${log.km.roundToInt()} $unit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$source \u00B7 ${CarFormatters.formatDate(log.date)}", 
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (potentialLogs.isNotEmpty()) {
                Button(
                    onClick = { onConfirm(selectedLogs.toList()) },
                    enabled = selectedLogs.isNotEmpty()
                ) {
                    Text(stringResource(R.string.common_apply_selected, selectedLogs.size))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (potentialLogs.isEmpty()) "Close" else stringResource(R.string.common_cancel))
            }
        }
    )
}
