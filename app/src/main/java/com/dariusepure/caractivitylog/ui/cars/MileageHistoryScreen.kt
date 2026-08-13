package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.History
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
    viewModel: MileageHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    var showAddMileageDialog by remember { mutableStateOf(false) }
    var showLogImportDialog by remember { mutableStateOf(false) }
    var editingMileageLog by remember { mutableStateOf<MileageLog?>(null) }
    var logToDelete by remember { mutableStateOf<MileageLog?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    if (showLogImportDialog) {
        val successState = state as? MileageHistoryUiState.Success
        if (successState != null) {
            LogImportDialog(
                fuelLogs = successState.fuelLogs,
                maintenanceLogs = successState.maintenanceLogs,
                inspections = successState.inspections,
                existingMileageLogs = successState.mileageLogs,
                unit = if (successState.unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL) "mi" else "km",
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
        val successState = state as? MileageHistoryUiState.Success
        val existingLogs = successState?.mileageLogs ?: emptyList()
        val unitLabel = if (successState?.unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL) "mi" else "km"

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
                val usesMiles = successState?.unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL
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
            MileageHistoryUiState.Loading -> LoadingState()
            is MileageHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is MileageHistoryUiState.Success -> {
                val usesMiles = s.unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL
                val unitLabel = if (usesMiles) "mi" else "km"
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        MileageStatsCard(s.stats, unitLabel)
                        
                        if (s.mileageLogs.size >= 2) {
                            Spacer(Modifier.height(8.dp))
                            val chartData = s.mileageLogs.map { 
                                val displayValue = CarFormatters.fromCanonicalDistance(it.km, usesMiles)
                                it.date to displayValue 
                            }
                            MileageLineChart(
                                data = chartData,
                                unit = unitLabel,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.mileage_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (s.mileageLogs.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.History,
                                title = stringResource(R.string.mileage_empty),
                                subtitle = ""
                            )
                        }
                    } else {
                        items(s.mileageLogs) { log ->
                            MileageLogItem(
                                log = log,
                                unit = unitLabel,
                                usesMiles = usesMiles,
                                onEdit = { editingMileageLog = log },
                                onDelete = { logToDelete = log }
                            )
                        }
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun MileageStatsCard(stats: MileageStats, unit: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = stringResource(R.string.pdf_field_nickname), 
                    value = stringResource(R.string.car_mileage_history)
                )
                StatItem(
                    label = "Total Records", 
                    value = "${stats.totalRecords}"
                )
            }
            Spacer(Modifier.height(16.dp))
            StatItem(
                label = "Current Mileage", 
                value = "${stats.currentMileage.roundToInt()} $unit"
            )
        }
    }
}

@Composable
fun MileageLogItem(
    log: com.dariusepure.caractivitylog.domain.MileageLog,
    unit: String,
    usesMiles: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dateFormat.format(log.date), style = MaterialTheme.typography.labelSmall)
                val displayValue = CarFormatters.fromCanonicalDistance(log.km, usesMiles)
                Text(
                    text = "${displayValue.roundToInt()} $unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            ActionButtons(
                onEdit = onEdit,
                onDelete = onDelete
            )
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
        val list = mutableListOf<Pair<MileageLog, String>>() // Log + Source Key
        
        val existingKms = existingMileageLogs.map { it.km.roundToInt() }.toSet()
        
        fuelLogs.forEach { fuel ->
            if (fuel.km.roundToInt() !in existingKms) {
                list.add(MileageLog(km = fuel.km, date = fuel.date) to "FUEL")
            }
        }
        
        maintenanceLogs.forEach { service ->
            if (service.km.roundToInt() !in existingKms) {
                list.add(MileageLog(km = service.km, date = service.date) to "SERVICE")
            }
        }

        inspections.forEach { inspection ->
            if (inspection.mileage.roundToInt() !in existingKms) {
                list.add(MileageLog(km = inspection.mileage, date = inspection.date) to "INSPECTION")
            }
        }
        
        list.distinctBy { it.first.km.roundToInt() }.sortedByDescending { it.first.date }
    }

    var selectedLogs by remember { mutableStateOf(potentialLogs.map { it.first }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mileage_import_title)) },
        text = {
            Column {
                if (potentialLogs.isEmpty()) {
                    Text(stringResource(R.string.mileage_import_empty))
                } else {
                    Text(
                        text = stringResource(R.string.mileage_import_subtitle, potentialLogs.size),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(potentialLogs) { (log, sourceKey) ->
                            val isSelected = log in selectedLogs
                            val sourceLabel = when(sourceKey) {
                                "FUEL" -> stringResource(R.string.mileage_source_fuel)
                                "SERVICE" -> stringResource(R.string.mileage_source_service)
                                "INSPECTION" -> stringResource(R.string.mileage_source_inspection)
                                else -> sourceKey
                            }
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
                                        val displayValue = CarFormatters.fromCanonicalDistance(log.km, unit == "mi")
                                        Text(
                                            text = "${displayValue.roundToInt()} $unit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$sourceLabel \u00B7 ${CarFormatters.formatDate(log.date)}", 
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
                Text(if (potentialLogs.isEmpty()) stringResource(R.string.common_cancel) else stringResource(R.string.common_cancel))
            }
        }
    )
}

