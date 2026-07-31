package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyCode by themeViewModel.currencyCode.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<Maintenance?>(null) }
    var recordToDelete by remember { mutableStateOf<Maintenance?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    if (showAddDialog || editingRecord != null) {
        val successState = state as? CarDetailsUiState.Success
        val existingLogs = successState?.mileageLogs ?: emptyList()
        val carAccentColor = successState?.car?.accentColor?.let { Color(it) } ?: Color(0xFF2196F3)

        AddServiceDialog(
            existingRecord = editingRecord,
            existingLogs = existingLogs,
            unit = successState?.let { s ->
                val country = europeanCountries.find { it.code == s.car.plateCountry }
                if (country?.usesMiles == true) "mi" else "km"
            } ?: "km",
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
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val carAccentColor = s.car.accentColor?.let { Color(it) } ?: Color(0xFF2196F3)
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = s.car.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (s.maintenanceLogs.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.service_empty),
                                modifier = Modifier.padding(vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(s.maintenanceLogs) { record ->
                            ServiceItem(
                                record = record,
                                unit = europeanCountries.find { it.code == s.car.plateCountry }?.let { if (it.usesMiles) "mi" else "km" } ?: "km",
                                currencyCode = currencyCode,
                                accentColor = carAccentColor,
                                onEdit = { editingRecord = record },
                                onDelete = { recordToDelete = record }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceItem(
    record: Maintenance,
    unit: String,
    currencyCode: String,
    accentColor: Color = Color(0xFF2196F3),
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(record.description, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${CarFormatters.formatDate(record.date)} \u00B7 ${record.km.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.cost > 0) {
                Text(
                    text = CarFormatters.formatCost(record.cost, currencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        val editTooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(R.string.common_edit))
                }
            },
            state = editTooltipState
        ) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = accentColor)
            }
        }
        
        val deleteTooltipState = rememberTooltipState()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(R.string.common_delete))
                }
            },
            state = deleteTooltipState
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

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
    var description by remember { mutableStateOf(existingRecord?.description ?: "") }
    var km by remember { mutableStateOf(existingRecord?.km?.roundToInt()?.toString() ?: "") }
    var cost by remember { mutableStateOf(existingRecord?.cost?.takeIf { it > 0 }?.toString() ?: "") }
    var date by remember { mutableStateOf(existingRecord?.date ?: Date()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
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
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.service_description_label)) },
                    placeholder = { Text(stringResource(R.string.service_description_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = km,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            km = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.service_mileage_label, unit)) },
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
                    value = cost,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) cost = it },
                    label = { Text(stringResource(R.string.common_cost_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

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
                    val usesMiles = unit == "mi"
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
                                description = description,
                                km = k,
                                cost = cost.toDoubleOrNull() ?: 0.0,
                                date = date
                            )
                        )
                    }
                },
                enabled = description.isNotBlank() && km.isNotBlank(),
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
