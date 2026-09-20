package com.dariusepure.caractivitylog.ui.cars

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dariusepure.caractivitylog.ui.common.DropdownPositionProvider
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun AddMileageDialog(
    existingLog: MileageLog? = null,
    existingLogs: List<MileageLog> = emptyList(),
    unit: String = "km",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onDismiss: () -> Unit,
    onConfirm: (Double, Date) -> Unit
) {
    val usesMiles = unit == "mi"
    val initialKm = existingLog?.let { CarFormatters.fromCanonicalDistance(it.km, usesMiles) }
    var km by remember { mutableStateOf(initialKm?.roundToInt()?.toString() ?: "") }
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
        title = { Text(if (existingLog == null) stringResource(R.string.mileage_add_title) else stringResource(R.string.mileage_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = km,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            km = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.common_mileage)) },
                    suffix = { Text(unit) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF1A73E8))
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inputVal = km.toDoubleOrNull() ?: 0.0
                    if (inputVal > 0) {
                        val canonicalInput = CarFormatters.toCanonicalDistance(inputVal, unit == "mi")
                        
                        val conflict = existingLogs.find { log ->
                            if (log.id == existingLog?.id) return@find false
                            val kmBackwards = selectedDate.after(log.date) && canonicalInput < log.km
                            val dateBackwards = selectedDate.before(log.date) && canonicalInput > log.km
                            kmBackwards || dateBackwards
                        }

                        if (conflict != null) {
                            val conflictDisplay = CarFormatters.fromCanonicalDistance(conflict.km, unit == "mi")
                            errorMessage = if (selectedDate.after(conflict.date)) {
                                context.getString(R.string.mileage_conflict_less, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                            } else {
                                context.getString(R.string.mileage_conflict_more, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                            }
                        } else {
                            onConfirm(inputVal, selectedDate)
                        }
                    }
                },
                enabled = km.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                )
            ) {
                Text(if (existingLog == null) stringResource(R.string.common_add) else stringResource(R.string.common_update))
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
fun AddInspectionDialog(
    existingInspection: VehicleInspection? = null,
    existingLogs: List<MileageLog> = emptyList(),
    unit: String = "km",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onDismiss: () -> Unit,
    onConfirm: (VehicleInspection) -> Unit
) {
    val usesMiles = unit == "mi"
    val initialKm = existingInspection?.let { CarFormatters.fromCanonicalDistance(it.mileage, usesMiles) }
    var km by remember { mutableStateOf(initialKm?.roundToInt()?.toString() ?: "") }
    var selectedDate by remember { mutableStateOf(existingInspection?.date ?: Date()) }
    var durationValue by remember { mutableStateOf(existingInspection?.durationValue?.toString() ?: "1") }
    var durationUnit by remember { mutableStateOf(existingInspection?.durationUnit ?: InspectionDurationUnit.YEARS) }
    var unitExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var unitMenuWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

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
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inspection_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = km,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            km = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.inspection_mileage_label)) },
                    suffix = { Text(unit) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dateFormat.format(selectedDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.inspection_date_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF1A73E8))
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationValue = it },
                        label = { Text(stringResource(R.string.inspection_validity_label)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = stringResource(durationUnit.labelRes),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.common_unit)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                            },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                .onGloballyPositioned { unitMenuWidth = with(density) { it.size.width.toDp() } },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (unitExpanded) {
                            Popup(
                                onDismissRequest = { unitExpanded = false },
                                popupPositionProvider = DropdownPositionProvider(),
                                properties = PopupProperties(focusable = false, clippingEnabled = false)
                            ) {
                                Surface(
                                    modifier = Modifier.width(unitMenuWidth),
                                    shape = RoundedCornerShape(4.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 3.dp
                                ) {
                                    Column {
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
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inputVal = km.toDoubleOrNull() ?: 0.0
                    val canonicalInput = CarFormatters.toCanonicalDistance(inputVal, unit == "mi")
                    
                    val conflict = existingLogs.find { log ->
                        val kmBackwards = selectedDate.after(log.date) && canonicalInput < log.km
                        val dateBackwards = selectedDate.before(log.date) && canonicalInput > log.km
                        kmBackwards || dateBackwards
                    }

                    if (conflict != null) {
                        val conflictDisplay = CarFormatters.fromCanonicalDistance(conflict.km, unit == "mi")
                        errorMessage = if (selectedDate.after(conflict.date)) {
                            context.getString(R.string.mileage_conflict_less, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                        } else {
                            context.getString(R.string.mileage_conflict_more, conflictDisplay.roundToInt(), unit, dateFormat.format(conflict.date))
                        }
                    } else {
                        onConfirm(
                            VehicleInspection(
                                id = existingInspection?.id ?: "",
                                date = selectedDate,
                                mileage = canonicalInput,
                                durationValue = durationValue.toIntOrNull() ?: 1,
                                durationUnit = durationUnit,
                                mileageLogId = existingInspection?.mileageLogId ?: ""
                            )
                        )
                    }
                },
                enabled = km.isNotBlank() && durationValue.isNotBlank(),
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
