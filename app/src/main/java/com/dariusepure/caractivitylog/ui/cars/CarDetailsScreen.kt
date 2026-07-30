package com.dariusepure.caractivitylog.ui.cars

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.*
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
import com.dariusepure.caractivitylog.ui.common.toRelativeString
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.util.LocalImageHelper
import com.dariusepure.caractivitylog.util.PdfReportGenerator
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
    onInsuranceClick: () -> Unit,
    onVignetteClick: () -> Unit,
    onTechnicalSheetClick: () -> Unit,
    onDiagnosisClick: () -> Unit,
    onFuelClick: () -> Unit,
    onTireClick: () -> Unit,
    onServiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.uploadImage(context, carId, it)
        }
    }

    var showImageDeleteDialog by remember { mutableStateOf(false) }

    if (showImageDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.removeImage(context, carId)
                showImageDeleteDialog = false
            },
            onDismiss = { showImageDeleteDialog = false }
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                val success = (state as? CarDetailsUiState.Success)?.let { s ->
                    try {
                        context.contentResolver.openOutputStream(it)?.use { os ->
                            PdfReportGenerator.generateReport(
                                context, s.car, s.mileageLogs, s.inspections, s.fuelLogs, s.tireSets, s.maintenanceLogs, os
                            )
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                } ?: false
                
                if (success) {
                    Toast.makeText(context, context.getString(R.string.car_report_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.car_report_failed), Toast.LENGTH_SHORT).show()
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

    val carAccentColor = (state as? CarDetailsUiState.Success)?.car?.accentColor?.let { Color(it) } 
        ?: Color(0xFF2196F3) // Default light blue
    val carOnAccentColor = Color.White

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                                    car = car,
                                    isUploading = s.isUploading,
                                    carAccentColor = carAccentColor,
                                    onPhotoClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onDeleteClick = { showImageDeleteDialog = true },
                                    context = context
                                )
                                Spacer(Modifier.width(24.dp))
                                CarHeaderText(car = car, context = context)
                            }
                        } else {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CarHeaderPhoto(
                                        car = car,
                                        isUploading = s.isUploading,
                                        carAccentColor = carAccentColor,
                                        onPhotoClick = {
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onDeleteClick = { showImageDeleteDialog = true },
                                        context = context
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    CarHeaderText(car = car, context = context)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // Bento Rows
                    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
                    
                    if (isExpanded) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_technical_sheet), style = MaterialTheme.typography.titleMedium)
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
                                    Text(text = stringResource(R.string.car_mileage_history), style = MaterialTheme.typography.titleMedium)
                                }
                                val latestInspection = s.inspections.maxByOrNull { it.date }
                                val isItpExpired = CarFormatters.isInspectionExpired(latestInspection)
                                val itpDays = latestInspection?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_inspection_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (isItpExpired) "Expired" else if (itpDays < 14) "Soon" else "OK",
                                        color = if (isItpExpired) MaterialTheme.colorScheme.error else if (itpDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val latestInsurance = s.insurances.maxByOrNull { it.date }
                                val isRcaExpired = latestInsurance?.expiryDate?.before(Date()) ?: true
                                val rcaDays = latestInsurance?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_insurance_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (isRcaExpired) "Expired" else if (rcaDays < 14) "Soon" else "OK",
                                        color = if (isRcaExpired) MaterialTheme.colorScheme.error else if (rcaDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }
                                val latestVignette = s.vignettes.maxByOrNull { it.date }
                                val isVigExpired = latestVignette?.expiryDate?.before(Date()) ?: true
                                val vigDays = latestVignette?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_vignette_title), style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(
                                        label = if (isVigExpired) "Expired" else if (vigDays < 14) "Soon" else "OK",
                                        color = if (isVigExpired) MaterialTheme.colorScheme.error else if (vigDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                        Icon(Icons.Default.LocalGasStation, null, modifier = Modifier.size(20.dp), tint = carAccentColor)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_fuel_consumption), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.service_history_title), style = MaterialTheme.typography.titleMedium)
                                }
                                BentoCard(
                                    onClick = onTireClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.tire_management_title), style = MaterialTheme.typography.titleMedium)
                                }
                                BentoCard(
                                    onClick = onDiagnosisClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Psychology, null, modifier = Modifier.size(48.dp), tint = carAccentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = stringResource(R.string.car_diagnosis_title), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    } else {
                        // Bento Row 1: Technical & Mileage
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_technical_sheet),
                                        style = MaterialTheme.typography.titleMedium,
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
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp), tint = carAccentColor)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_mileage_history),
                                        style = MaterialTheme.typography.titleMedium,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Bento Row 2: ITP, RCA, Vignette (Squares)
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val latestInspection = s.inspections.maxByOrNull { it.date }
                                val isItpExpired = CarFormatters.isInspectionExpired(latestInspection)
                                val itpDays = latestInspection?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1
                                
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, null, modifier = Modifier.size(56.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_inspection_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (isItpExpired) "Expired" else if (itpDays < 14) "Soon" else "OK",
                                        color = if (isItpExpired) MaterialTheme.colorScheme.error else if (itpDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }

                                val latestInsurance = s.insurances.maxByOrNull { it.date }
                                val isRcaExpired = latestInsurance?.expiryDate?.before(Date()) ?: true
                                val rcaDays = latestInsurance?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1

                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(56.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_insurance_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (isRcaExpired) "Expired" else if (rcaDays < 14) "Soon" else "OK",
                                        color = if (isRcaExpired) MaterialTheme.colorScheme.error else if (rcaDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }

                                val latestVignette = s.vignettes.maxByOrNull { it.date }
                                val isVigExpired = latestVignette?.expiryDate?.before(Date()) ?: true
                                val vigDays = latestVignette?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) } ?: -1

                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(56.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_vignette_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                    StatusBadge(
                                        label = if (isVigExpired) "Expired" else if (vigDays < 14) "Soon" else "OK",
                                        color = if (isVigExpired) MaterialTheme.colorScheme.error else if (vigDays < 14) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }

                        // Bento Row 3: Fuel & Service
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                        Icon(Icons.Default.LocalGasStation, null, modifier = Modifier.size(24.dp), tint = carAccentColor)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_fuel_consumption),
                                        style = MaterialTheme.typography.titleMedium,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                                
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.service_history_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Tires & Diagnosis
                        item {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BentoCard(
                                    onClick = onTireClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.tire_management_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }

                                BentoCard(
                                    onClick = onDiagnosisClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Default.Psychology, null, modifier = Modifier.size(52.dp), tint = carAccentColor)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_diagnosis_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    // Report (Full Width)
                    item {
                        BentoCard(
                            onClick = { 
                                val fileName = "Report_${car.displayName.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())}.pdf"
                                pdfLauncher.launch(fileName) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = carAccentColor
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(40.dp), tint = carOnAccentColor)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.car_generate_report), style = MaterialTheme.typography.titleMedium, color = carOnAccentColor)
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
                                date = selectedDate,
                                mileage = canonicalInput,
                                durationValue = durationValue.toIntOrNull() ?: 1,
                                durationUnit = durationUnit
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
    car: Car,
    isUploading: Boolean,
    carAccentColor: Color,
    onPhotoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    context: android.content.Context
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                if (!isUploading) {
                    onPhotoClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = carAccentColor
            )
        } else {
            val localImage = remember(car.id, isUploading) {
                LocalImageHelper.getCarImageFile(context, car.id)
            }

            if (localImage != null) {
                AsyncImage(
                    model = localImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Small delete overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        modifier = Modifier.size(16.dp).clickable { onDeleteClick() },
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            modifier = Modifier.padding(2.dp),
                            tint = Color.White
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = carAccentColor
                )
            }
        }
    }
}

@Composable
private fun CarHeaderText(car: Car, context: android.content.Context) {
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
