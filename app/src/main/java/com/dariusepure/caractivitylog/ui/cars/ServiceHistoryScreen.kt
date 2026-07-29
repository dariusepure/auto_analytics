package com.dariusepure.caractivitylog.ui.cars

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
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<Maintenance?>(null) }
    var recordToDelete by remember { mutableStateOf<Maintenance?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    if (showAddDialog || editingRecord != null) {
        AddServiceDialog(
            existingRecord = editingRecord,
            unit = (state as? CarDetailsUiState.Success)?.let { s ->
                val country = europeanCountries.find { it.code == s.car.plateCountry }
                if (country?.usesMiles == true) "mi" else "km"
            } ?: "km",
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
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.service_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMaintenance(carId, recordToDelete!!)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
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

@Composable
fun ServiceItem(
    record: Maintenance,
    unit: String,
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
                    text = "${record.cost}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, null, tint = Color(0xFF2196F3))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = Color.Red)
        }
    }
}

@Composable
fun AddServiceDialog(
    existingRecord: Maintenance? = null,
    unit: String = "km",
    onDismiss: () -> Unit,
    onConfirm: (Maintenance) -> Unit
) {
    var description by remember { mutableStateOf(existingRecord?.description ?: "") }
    var km by remember { mutableStateOf(existingRecord?.km?.roundToInt()?.toString() ?: "") }
    var cost by remember { mutableStateOf(existingRecord?.cost?.takeIf { it > 0 }?.toString() ?: "") }
    var date by remember { mutableStateOf(existingRecord?.date ?: Date()) }

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
                    onValueChange = { if (it.all { char -> char.isDigit() }) km = it },
                    label = { Text(stringResource(R.string.service_mileage_label, unit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) cost = it },
                    label = { Text(stringResource(R.string.common_cost_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Maintenance(
                            description = description,
                            km = km.toDoubleOrNull() ?: 0.0,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            date = date
                        )
                    )
                },
                enabled = description.isNotBlank() && km.isNotBlank()
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
