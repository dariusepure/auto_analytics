package com.dariusepure.caractivitylog.ui.cars

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.util.PdfReportGenerator
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsScreen(
    carId: String,
    onBack: () -> Unit,
    onMileageClick: () -> Unit,
    onInspectionClick: () -> Unit,
    onTechnicalSheetClick: () -> Unit,
    onDiagnosisClick: () -> Unit,
    onFuelClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                val success = (state as? CarDetailsUiState.Success)?.let { s ->
                    try {
                        context.contentResolver.openOutputStream(it)?.use { os ->
                            PdfReportGenerator.generateReport(
                                context, s.car, s.mileageLogs, s.inspections, s.fuelLogs, os
                            )
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                } ?: false
                
                if (success) {
                    Toast.makeText(context, "Report saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save report", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
    
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


    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Car Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(message = s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val car = s.car

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val logoRes = BrandHelper.getLogoResource(context, car.make)
                            if (logoRes != 0) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = car.make,
                                    modifier = Modifier.size(64.dp), // Increased from 40dp
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(16.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Text(
                                text = car.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            onClick = onTechnicalSheetClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Technical Sheet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "View full specifications",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        
                        Card(
                            onClick = onMileageClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Mileage History",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "View and manage records",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))

                        val latestInspection = s.inspections.maxByOrNull { it.date }
                        val isExpired = CarFormatters.isInspectionExpired(latestInspection)

                        Card(
                            onClick = onInspectionClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Vehicle Inspection (ITP)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = CarFormatters.getInspectionExpiryText(latestInspection),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExpired) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Card(
                            onClick = onDiagnosisClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Psychology, 
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AI Diagnosis Chat",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Get expert advice and health reports",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))

                        Card(
                            onClick = onFuelClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocalGasStation, 
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Fuel Consumption",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Track fillings and view stats",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Card(
                            onClick = { 
                                val fileName = "Report_${car.displayName.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())}.pdf"
                                pdfLauncher.launch(fileName) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Generate PDF Report",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Export vehicle history to PDF",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        
                        Spacer(Modifier.height(80.dp)) // Space for FAB
                    }
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
        title = { Text(if (existingLog == null) "Add Mileage" else "Edit Mileage") },
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
                    label = { Text("Distance ($unit)") },
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
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Select Date")
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
                                "Cannot be less than ${conflictDisplay.roundToInt()} $unit (recorded on ${dateFormat.format(conflict.date)})"
                            } else {
                                "Cannot be more than ${conflictDisplay.roundToInt()} $unit (recorded on ${dateFormat.format(conflict.date)})"
                            }
                        } else {
                            onConfirm(inputVal, selectedDate)
                        }
                    }
                },
                enabled = km.isNotBlank()
            ) {
                Text(if (existingLog == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddInspectionDialog(
    existingInspection: VehicleInspection? = null,
    unit: String = "km",
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
        title = { Text("Add Vehicle Inspection") },
        text = {
            Column {
                OutlinedTextField(
                    value = km,
                    onValueChange = { if (it.all { char -> char.isDigit() }) km = it },
                    label = { Text("Mileage (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dateFormat.format(selectedDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Inspection Date") },
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationValue = it },
                        label = { Text("Validity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = durationUnit.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Unit") },
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
                                    text = { Text(unit.name.lowercase().replaceFirstChar { it.uppercase() }) },
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
                    val canonicalValue = CarFormatters.toCanonicalDistance(inputVal, unit == "mi")
                    onConfirm(
                        VehicleInspection(
                            date = selectedDate,
                            mileage = canonicalValue,
                            durationValue = durationValue.toIntOrNull() ?: 1,
                            durationUnit = durationUnit
                        )
                    )
                },
                enabled = km.isNotBlank() && durationValue.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
