package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.ui.common.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.dariusepure.caractivitylog.ui.theme.statusExpiredRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: InspectionHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showLogImportDialog by remember { mutableStateOf(false) }
    var editingInspection by remember { mutableStateOf<VehicleInspection?>(null) }
    var inspectionToDelete by remember { mutableStateOf<VehicleInspection?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    if (showLogImportDialog) {
        val successState = state as? InspectionHistoryUiState.Success
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

    if (inspectionToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteInspection(carId, inspectionToDelete!!)
                inspectionToDelete = null
            },
            onDismiss = { inspectionToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.inspection_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showLogImportDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.mileage_import_action))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.inspection_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            InspectionHistoryUiState.Loading -> LoadingState()
            is InspectionHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is InspectionHistoryUiState.Success -> {
                val usesMiles = s.unitSystem == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL
                val unit = if (usesMiles) "mi" else "km"
                val carAccentColor = Color(0xFF1A73E8)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                        val statusColor = when {
                            s.stats.daysRemaining == null -> MaterialTheme.colorScheme.primary
                            s.stats.daysRemaining < 0 -> statusExpiredRed
                            s.stats.daysRemaining < 14 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(
                                label = stringResource(R.string.stats_valid_until), 
                                value = s.stats.latestExpiryDate?.let { dateFormat.format(it) } ?: "--",
                                color = statusColor
                            )
                            StatItem(
                                label = stringResource(R.string.stats_remaining), 
                                value = s.stats.daysRemaining?.let { if (it < 0) stringResource(R.string.common_expired) else stringResource(R.string.stats_days_suffix, it) } ?: "--",
                                color = statusColor
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.inspection_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (s.inspections.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.AssignmentTurnedIn,
                                title = stringResource(R.string.inspection_empty),
                                subtitle = ""
                            )
                        }
                    }

                    items(s.inspections) { inspection ->
                        InspectionLogItem(
                            inspection = inspection,
                            unit = unit,
                            usesMiles = usesMiles,
                            onEdit = { editingInspection = inspection },
                            onDelete = { inspectionToDelete = inspection }
                        )
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showAddDialog || editingInspection != null) {
                    AddInspectionDialog(
                        existingInspection = editingInspection,
                        existingLogs = s.mileageLogs,
                        unit = unit,
                        accentColor = carAccentColor,
                        onAccentColor = Color.White,
                        onDismiss = { 
                            showAddDialog = false
                            editingInspection = null
                        },
                        onConfirm = { inspection: VehicleInspection ->
                            if (editingInspection != null) {
                                viewModel.updateInspection(carId, inspection.copy(id = editingInspection!!.id, mileageLogId = editingInspection!!.mileageLogId))
                            } else {
                                viewModel.addInspection(carId, inspection)
                            }
                            showAddDialog = false
                            editingInspection = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InspectionLogItem(
    inspection: VehicleInspection,
    unit: String,
    usesMiles: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AssignmentTurnedIn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dateFormat.format(inspection.date), style = MaterialTheme.typography.labelSmall)
                Text(
                    text = stringResource(R.string.common_inspection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val displayKm = CarFormatters.fromCanonicalDistance(inspection.mileage, usesMiles)
                Text(
                    text = "${displayKm.roundToInt()} $unit \u00B7 ${stringResource(R.string.common_expires)}: ${dateFormat.format(inspection.expiryDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            ActionButtons(
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}
