package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.ui.theme.statusExpiredRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceHistoryScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: InsuranceHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInsurance by remember { mutableStateOf<Insurance?>(null) }
    var insuranceToDelete by remember { mutableStateOf<Insurance?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadData(carId)
    }

    if (insuranceToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteInsurance(carId, insuranceToDelete!!.id)
                insuranceToDelete = null
            },
            onDismiss = { insuranceToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.insurance_history_title)) },
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.insurance_add_title))
            }
        }
    ) { padding ->
        when (val s = state) {
            InsuranceHistoryUiState.Loading -> LoadingState()
            is InsuranceHistoryUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadData(carId) })
            is InsuranceHistoryUiState.Success -> {
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
                            text = stringResource(R.string.insurance_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (s.insurances.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.Security,
                                title = stringResource(R.string.insurance_empty),
                                subtitle = ""
                            )
                        }
                    }

                    items(s.insurances) { insurance ->
                        InsuranceLogItem(
                            insurance = insurance,
                            onEdit = { editingInsurance = insurance },
                            onDelete = { insuranceToDelete = insurance }
                        )
                    }
                    
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showAddDialog || editingInsurance != null) {
                    AddInsuranceDialog(
                        existingInsurance = editingInsurance,
                        onDismiss = { 
                            showAddDialog = false
                            editingInsurance = null
                        },
                        onConfirm = { insurance: Insurance ->
                            if (editingInsurance != null) {
                                viewModel.updateInsurance(carId, insurance.copy(id = editingInsurance!!.id))
                            } else {
                                viewModel.addInsurance(carId, insurance)
                            }
                            showAddDialog = false
                            editingInsurance = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddInsuranceDialog(
    existingInsurance: Insurance? = null,
    onDismiss: () -> Unit,
    onConfirm: (Insurance) -> Unit
) {
    var selectedDate by remember { mutableStateOf(existingInsurance?.date ?: Date()) }
    var durationValue by remember { mutableStateOf(existingInsurance?.durationValue?.toString() ?: "1") }
    var durationUnit by remember { mutableStateOf(existingInsurance?.durationUnit ?: InspectionDurationUnit.YEARS) }
    var provider by remember { mutableStateOf(existingInsurance?.provider ?: "") }
    var unitExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val calendar = Calendar.getInstance()
    calendar.time = selectedDate
    
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDate = newCalendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.insurance_add_title)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text(stringResource(R.string.insurance_provider_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateFormat.format(selectedDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.common_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        }
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationValue = it },
                        label = { Text(stringResource(R.string.inspection_validity_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = stringResource(durationUnit.labelRes),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.common_unit)) },
                            trailingIcon = {
                                IconButton(onClick = { unitExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable { unitExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            InspectionDurationUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(unit.labelRes)) },
                                    onClick = {
                                        durationUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Insurance(
                            date = selectedDate,
                            durationValue = durationValue.toIntOrNull() ?: 1,
                            durationUnit = durationUnit,
                            provider = provider
                        )
                    )
                },
                enabled = durationValue.isNotBlank() && provider.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
fun InsuranceLogItem(
    insurance: Insurance,
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
            Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dateFormat.format(insurance.date), style = MaterialTheme.typography.labelSmall)
                Text(
                    text = insurance.provider.ifBlank { "Insurance Policy" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stringResource(R.string.common_expires)}: ${dateFormat.format(insurance.expiryDate)}",
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
