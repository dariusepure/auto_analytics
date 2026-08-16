package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Build
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
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: ServiceHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<Maintenance?>(null) }
    var recordToDelete by remember { mutableStateOf<Maintenance?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    if (recordToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteMaintenance(carId, recordToDelete!!)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.service_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            ServiceHistoryUiState.Loading -> LoadingState()
            is ServiceHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is ServiceHistoryUiState.Success -> {
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
                        Spacer(Modifier.height(8.dp))
                    }

                    if (s.logs.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.Build,
                                title = stringResource(R.string.service_empty),
                                subtitle = ""
                            )
                        }
                    }

                    items(
                        items = s.logs,
                        key = { it.id },
                        contentType = { "service_log" }
                    ) { record ->
                        ServiceLogItem(
                            record = record,
                            unit = unit,
                            usesMiles = usesMiles,
                            accentColor = carAccentColor,
                            onEdit = { editingRecord = record },
                            onDelete = { recordToDelete = record }
                        )
                    }
                    
                    item(contentType = { "spacer" }) { Spacer(Modifier.height(80.dp)) }
                }

                if (showAddDialog || editingRecord != null) {
                    AddServiceDialog(
                        existingRecord = editingRecord,
                        existingLogs = s.mileageLogs,
                        unit = unit,
                        accentColor = carAccentColor,
                        onAccentColor = Color.White,
                        onDismiss = {
                            showAddDialog = false
                            editingRecord = null
                        },
                        onConfirm = { record ->
                            if (editingRecord != null) {
                                viewModel.updateMaintenance(carId, record.copy(id = editingRecord!!.id, mileageLogId = editingRecord!!.mileageLogId))
                            } else {
                                viewModel.addMaintenance(carId, record)
                            }
                            showAddDialog = false
                            editingRecord = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    existingRecord: Maintenance? = null,
    existingLogs: List<com.dariusepure.caractivitylog.domain.MileageLog> = emptyList(),
    unit: String = "km",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onDismiss: () -> Unit,
    onConfirm: (Maintenance) -> Unit
) {
    val serviceOperations = listOf(
        "Oil and Filter Change",
        "Air Filter Replacement",
        "Cabin Filter Replacement",
        "Fuel Filter Replacement",
        "Brake Pads Replacement",
        "Brake Discs Replacement",
        "Timing Belt / Water Pump Kit",
        "Clutch Kit Replacement",
        "Battery Replacement",
        "Suspension Overhaul",
        "Wheel Alignment",
        "AC Recharge (Freon)",
        "Spark Plugs Replacement",
        "Engine Overhaul",
        "Injectors Cleaning/Replacement",
        "Turbocharger Repair/Replacement",
        "Transmission Oil Change",
        "Brake Fluid Change",
        "Coolant (Antifreeze) Change",
        "DPF / EGR Cleaning",
        "Accessory Belt Replacement",
        "Shock Absorbers Replacement",
        "Steering System Repair",
        "Computer Diagnosis (Tester)",
        "Other (Manual Entry)"
    )

    var selectedOperation by remember { 
        mutableStateOf(
            if (existingRecord == null) "" 
            else if (existingRecord.description in serviceOperations) existingRecord.description 
            else "Other (Manual Entry)"
        ) 
    }
    var customDescription by remember { 
        mutableStateOf(if (selectedOperation == "Other (Manual Entry)") existingRecord?.description ?: "" else "") 
    }
    
    var expanded by remember { mutableStateOf(false) }
    val usesMiles = unit == "mi"
    var km by remember { 
        mutableStateOf(
            existingRecord?.let { 
                CarFormatters.fromCanonicalDistance(it.km, usesMiles).roundToInt().toString() 
            } ?: ""
        ) 
    }
    var date by remember { mutableStateOf(existingRecord?.date ?: Date()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    val calendar = Calendar.getInstance()
    calendar.time = date

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            date = newCalendar.time
            errorMessage = null
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord == null) stringResource(R.string.service_add_title) else stringResource(R.string.service_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Dropdown for Service Operations
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedOperation.isNotEmpty()) CarTranslations.getServiceOperationLabel(context, selectedOperation) else "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.service_description_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        serviceOperations.forEach { operation ->
                            DropdownMenuItem(
                                text = { Text(CarTranslations.getServiceOperationLabel(context, operation)) },
                                onClick = {
                                    selectedOperation = operation
                                    expanded = false
                                    if (operation != "Other (Manual Entry)") {
                                        customDescription = ""
                                    }
                                }
                            )
                        }
                    }
                }

                // Show manual entry field if "Other" is selected
                if (selectedOperation == "Other (Manual Entry)") {
                    OutlinedTextField(
                        value = customDescription,
                        onValueChange = { customDescription = it },
                        label = { Text(stringResource(R.string.service_description_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = km,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            km = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.service_mileage_label)) },
                    suffix = { Text(unit) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    isError = errorMessage != null
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = dateFormat.format(date),
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
                    val canonicalInput = CarFormatters.toCanonicalDistance(k, usesMiles)
                    
                    val conflict = existingLogs.find { log ->
                        if (log.id == existingRecord?.mileageLogId) return@find false
                        val kmBackwards = date.after(log.date) && canonicalInput < log.km
                        val dateBackwards = date.before(log.date) && canonicalInput > log.km
                        kmBackwards || dateBackwards
                    }

                    if (conflict != null) {
                        val conflictDisplay = CarFormatters.fromCanonicalDistance(conflict.km, usesMiles)
                        errorMessage = if (date.after(conflict.date)) {
                            context.getString(R.string.mileage_conflict_less, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                        } else {
                            context.getString(R.string.mileage_conflict_more, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                        }
                    } else {
                        onConfirm(
                            Maintenance(
                                description = if (selectedOperation == "Other (Manual Entry)") customDescription else selectedOperation,
                                km = canonicalInput,
                                date = date
                            )
                        )
                    }
                },
                enabled = selectedOperation.isNotEmpty() && (selectedOperation != "Other (Manual Entry)" || customDescription.isNotBlank()) && km.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                )
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceLogItem(
    record: Maintenance,
    unit: String,
    usesMiles: Boolean,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dateFormat.format(record.date), style = MaterialTheme.typography.labelSmall)
                Text(
                    text = CarTranslations.getServiceOperationLabel(context, record.description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val displayKm = CarFormatters.fromCanonicalDistance(record.km, usesMiles)
                Text(
                    text = "${displayKm.roundToInt()} $unit",
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
