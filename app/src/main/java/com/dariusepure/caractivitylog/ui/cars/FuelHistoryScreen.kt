package com.dariusepure.caractivitylog.ui.cars

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.common.ConsumptionLineChart
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: FuelHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<com.dariusepure.caractivitylog.domain.FuelLog?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fuel_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fuel_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            FuelHistoryUiState.Loading -> LoadingState()
            is FuelHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is FuelHistoryUiState.Success -> {
                val country = europeanCountries.find { it.code == s.car.plateCountry }
                val usesMiles = country?.usesMiles == true
                val distUnit = if (usesMiles) "mi" else "km"
                val consUnit = if (usesMiles) "mpg" else "L/100km"

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FuelStatsCard(s.stats, distUnit, consUnit)
                        
                        if (s.logs.any { it.consumption != null }) {
                            val chartData = s.logs.mapNotNull { 
                                if (it.consumption != null) it.log.date to it.consumption else null 
                            }
                            ConsumptionLineChart(
                                data = chartData,
                                unit = consUnit,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }

                        if (s.stats.avgConsumption == null && s.logs.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.fuel_add_full_tank_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.fuel_history_header),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (s.logs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.DirectionsCar, 
                                    null, 
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.fuel_history_empty), color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    items(s.logs) { entry ->
                        FuelLogItem(
                            entry = entry,
                            distUnit = distUnit,
                            consUnit = consUnit,
                            usesMiles = usesMiles,
                            onEdit = { editingLog = entry.log },
                            onDelete = { viewModel.deleteFuelLog(carId, entry.log) }
                        )
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showAddDialog || editingLog != null) {
                    AddFuelDialog(
                        unit = distUnit,
                        usesMiles = usesMiles,
                        existingLog = editingLog,
                        existingLogs = s.mileageLogs,
                        onDismiss = { 
                            showAddDialog = false
                            editingLog = null
                        },
                        onConfirm = { kmInput, liters, cost, isFull, date ->
                            val kmCanonical = CarFormatters.toCanonicalDistance(kmInput, usesMiles)
                            
                            val logToEdit = editingLog
                            if (logToEdit != null) {
                                viewModel.updateFuelLog(carId, logToEdit.copy(
                                    km = kmCanonical,
                                    liters = liters,
                                    cost = cost,
                                    isFullTank = isFull,
                                    date = date
                                ))
                            } else {
                                viewModel.addFuelLog(carId, kmCanonical, liters, cost, isFull, date)
                            }
                            showAddDialog = false
                            editingLog = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FuelStatsCard(stats: FuelStats, distUnit: String, consUnit: String) {
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
                    label = stringResource(R.string.fuel_stats_avg), 
                    value = stats.avgConsumption?.let { String.format(Locale.getDefault(), "%.2f %s", it, consUnit) } ?: "-- $consUnit"
                )
                StatItem(stringResource(R.string.fuel_stats_total_dist), "${stats.totalDistance.roundToInt()} $distUnit")
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(stringResource(R.string.fuel_stats_total_fuel), String.format(Locale.getDefault(), "%.1f L", stats.totalLiters))
                StatItem(stringResource(R.string.fuel_stats_total_cost), String.format(Locale.getDefault(), "%.2f", stats.totalCost))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun FuelLogItem(
    entry: FuelLogWithConsumption,
    distUnit: String,
    consUnit: String,
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
            Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dateFormat.format(entry.log.date), style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "${CarFormatters.fromCanonicalDistance(entry.log.km, usesMiles).roundToInt()} $distUnit",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${entry.log.liters} L \u00B7 ${entry.log.cost} \u00B7 ${if (entry.log.isFullTank) stringResource(R.string.fuel_full_tank_label) else stringResource(R.string.fuel_partial_tank)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (entry.consumption != null) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f", entry.consumption),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(consUnit, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    null,
                    tint = androidx.compose.ui.graphics.Color(0xFF2196F3)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddFuelDialog(
    unit: String,
    usesMiles: Boolean,
    existingLog: com.dariusepure.caractivitylog.domain.FuelLog? = null,
    existingLogs: List<MileageLog> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Boolean, Date) -> Unit
) {
    var km by remember { 
        mutableStateOf(existingLog?.let { CarFormatters.fromCanonicalDistance(it.km, usesMiles).roundToInt().toString() } ?: "") 
    }
    var liters by remember { mutableStateOf(existingLog?.liters?.toString() ?: "") }
    var cost by remember { mutableStateOf(existingLog?.cost?.toString() ?: "") }
    var isFullTank by remember { mutableStateOf(existingLog?.isFullTank ?: true) }
    var selectedDate by remember { mutableStateOf(existingLog?.date ?: Date()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val calendar = Calendar.getInstance()
    calendar.time = selectedDate

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDate = newCalendar.time
            errorMessage = null
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingLog == null) stringResource(R.string.fuel_add_title) else stringResource(R.string.common_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = km,
                    onValueChange = { 
                        if (it.all { c -> c.isDigit() }) {
                            km = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.common_distance, unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = liters,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) liters = it },
                        label = { Text(stringResource(R.string.fuel_liters_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) cost = it },
                        label = { Text(stringResource(R.string.fuel_cost_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFullTank, onCheckedChange = { isFullTank = it })
                    Text(stringResource(R.string.fuel_full_tank_label), modifier = Modifier.clickable { isFullTank = !isFullTank })
                }

                OutlinedTextField(
                    value = dateFormat.format(selectedDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.common_date)) },
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val k = km.toDoubleOrNull() ?: 0.0
                    val l = liters.toDoubleOrNull() ?: 0.0
                    val c = cost.toDoubleOrNull() ?: 0.0
                    
                    if (k > 0 && l > 0) {
                        val canonicalInput = CarFormatters.toCanonicalDistance(k, usesMiles)
                        
                        val conflict = existingLogs.find { log ->
                            if (log.id == existingLog?.mileageLogId) return@find false
                            val kmBackwards = selectedDate.after(log.date) && canonicalInput < log.km
                            val dateBackwards = selectedDate.before(log.date) && canonicalInput > log.km
                            kmBackwards || dateBackwards
                        }

                        if (conflict != null) {
                            errorMessage = context.getString(R.string.fuel_mileage_conflict, dateFormat.format(conflict.date))
                        } else {
                            onConfirm(k, l, c, isFullTank, selectedDate)
                        }
                    }
                },
                enabled = km.isNotBlank() && liters.isNotBlank()
            ) {
                Text(if (existingLog == null) stringResource(R.string.common_add) else stringResource(R.string.common_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
