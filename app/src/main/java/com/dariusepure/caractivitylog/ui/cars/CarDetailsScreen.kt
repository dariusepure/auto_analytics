/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.*
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
import com.dariusepure.caractivitylog.ui.common.toRelativeString
import com.dariusepure.caractivitylog.ui.common.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsScreen(
    carId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    onMileageClick: () -> Unit,
    onInspectionClick: () -> Unit,
    onInsuranceClick: () -> Unit,
    onVignetteClick: () -> Unit,
    onTireClick: () -> Unit,
    onServiceClick: () -> Unit,
    onTechnicalSheetClick: () -> Unit,
    onDiagnosisClick: () -> Unit,
    onFuelClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CarDetailsUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    val carAccentColor = Color(0xFF1A73E8) // Default light blue

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(carId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.common_edit),
                            tint = Color(0xFF1A73E8)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> CarDetailsSkeleton()
            is CarDetailsUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val car = s.car

                @Composable
                fun getStatusColor(expiryDate: Date?): Color {
                    if (expiryDate == null) return Color(0xFF1A73E8) // Default Blue
                    val now = Date()
                    val diff = expiryDate.time - now.time
                    val days = diff / (1000 * 60 * 60 * 24)
                    
                    return when {
                        expiryDate.before(now) -> MaterialTheme.colorScheme.error
                        days < 14 -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFF4CAF50) // Green
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
                        
                        if (isExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CarHeaderPhoto(
                                    carAccentColor = carAccentColor
                                )
                                Spacer(Modifier.width(24.dp))
                                CarHeaderText(
                                    car = car, 
                                    context = context
                                )
                            }
                        } else {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CarHeaderPhoto(
                                        carAccentColor = carAccentColor
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    CarHeaderText(
                                        car = car, 
                                        context = context
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // Bento Rows
                    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
                    
                    if (isExpanded) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_technical_sheet),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                                BentoCard(
                                    onClick = onMileageClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = carAccentColor)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_mileage_history),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                                val latestInspection = s.inspections.maxByOrNull { it.date }
                                val inspectionColor = getStatusColor(latestInspection?.expiryDate)
                                val itpDays = latestInspection?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = inspectionColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, null, modifier = Modifier.size(48.dp), tint = inspectionColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_inspection_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (latestInspection == null) stringResource(R.string.common_not_applicable) else if (itpDays < 0) stringResource(R.string.status_expired) else if (itpDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = inspectionColor
                                    )
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val latestInsurance = s.insurances.maxByOrNull { it.date }
                                val insuranceColor = getStatusColor(latestInsurance?.expiryDate)
                                val rcaDays = latestInsurance?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                
                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = insuranceColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(48.dp), tint = insuranceColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_insurance_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (latestInsurance == null) stringResource(R.string.common_not_applicable) else if (rcaDays < 0) stringResource(R.string.status_expired) else if (rcaDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = insuranceColor
                                    )
                                }
                                val latestVignette = s.vignettes.maxByOrNull { it.date }
                                val vignetteColor = getStatusColor(latestVignette?.expiryDate)
                                val vigDays = latestVignette?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                
                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = vignetteColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(48.dp), tint = vignetteColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_vignette_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (latestVignette == null) stringResource(R.string.common_not_applicable) else if (vigDays < 0) stringResource(R.string.status_expired) else if (vigDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = vignetteColor
                                    )
                                }
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.LocalGasStation, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_fuel_consumption),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    AutoSizeText(
                                        text = stringResource(R.string.service_history_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minFontSize = 9.sp
                                    )
                                }
                                BentoCard(
                                    onClick = onTireClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.TireRepair, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    AutoSizeText(
                                        text = stringResource(R.string.tire_management_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minFontSize = 9.sp
                                    )
                                }
                                BentoCard(
                                    onClick = onDiagnosisClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Engineering, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_diagnosis_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minFontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
else {
                        // Bento Row 1: Technical & Mileage
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_technical_sheet),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                                
                                BentoCard(
                                    onClick = onMileageClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp), tint = carAccentColor)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_mileage_history),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Bento Row 2: ITP, RCA, Vignette (Squares)
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val latestInspection = s.inspections.maxByOrNull { it.date }
                                val inspectionColor = getStatusColor(latestInspection?.expiryDate)
                                val itpDays = latestInspection?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = inspectionColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, null, modifier = Modifier.size(56.dp), tint = inspectionColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_inspection_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (latestInspection == null) stringResource(R.string.common_not_applicable) else if (itpDays < 0) stringResource(R.string.status_expired) else if (itpDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = inspectionColor
                                    )
                                }

                                val latestInsurance = s.insurances.maxByOrNull { it.date }
                                val insuranceColor = getStatusColor(latestInsurance?.expiryDate)
                                val rcaDays = latestInsurance?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1

                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = insuranceColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(56.dp), tint = insuranceColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_insurance_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (latestInsurance == null) stringResource(R.string.common_not_applicable) else if (rcaDays < 0) stringResource(R.string.status_expired) else if (rcaDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = insuranceColor
                                    )
                                }

                                val latestVignette = s.vignettes.maxByOrNull { it.date }
                                val vignetteColor = getStatusColor(latestVignette?.expiryDate)
                                val vigDays = latestVignette?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1

                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = vignetteColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(56.dp), tint = vignetteColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_vignette_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (latestVignette == null) stringResource(R.string.common_not_applicable) else if (vigDays < 0) stringResource(R.string.status_expired) else if (vigDays < 14) stringResource(R.string.status_soon) else stringResource(R.string.status_ok),
                                        color = vignetteColor
                                    )
                                }
                            }
                        }

                        // Bento Row 3: Fuel & Tires
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.LocalGasStation, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_fuel_consumption),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                                
                                BentoCard(
                                    onClick = onTireClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.TireRepair, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.tire_management_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Bento Row 4: Service & Diagnosis
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.service_history_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }

                                BentoCard(
                                    onClick = onDiagnosisClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Engineering, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_diagnosis_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

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
                    label = { Text(stringResource(R.string.common_distance, unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    label = { Text(stringResource(R.string.inspection_mileage_label, unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = durationUnit.name.lowercase().replaceFirstChar { it.uppercase() },
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

@Composable
private fun CarHeaderPhoto(
    carAccentColor: Color
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = carAccentColor
        )
    }
}

@Composable
private fun CarHeaderText(
    car: Car, 
    context: android.content.Context
) {
    Column {
        AutoSizeText(
            text = car.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (car.licensePlate.isNotBlank()) {
            Text(
                text = car.licensePlate,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = stringResource(R.string.car_last_update, car.updatedAt.toRelativeString(context)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


