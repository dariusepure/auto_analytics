package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.CarEquipment
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.ui.common.CheckEngineIcon
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.CarTranslations
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.PdfPreviewDialog
import com.dariusepure.caractivitylog.ui.common.SpecItem
import com.dariusepure.caractivitylog.ui.common.SpecificationCard
import com.dariusepure.caractivitylog.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalSheetScreen(
    carId: String,
    onBack: () -> Unit,
    onReportsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let { destUri ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                tempPdfFile?.inputStream()?.use { it.copyTo(os) }
                            }
                        }
                        snackbarHostState.showSnackbar(context.getString(R.string.car_report_success))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar(context.getString(R.string.car_report_failed))
                    } finally {
                        tempPdfFile?.delete()
                        tempPdfFile = null
                    }
                }
            }
        }
    )

    if (tempPdfFile != null) {
        val carName = (state as? CarDetailsUiState.Success)?.car?.let { "${it.make}_${it.model}" } ?: "Car"
        PdfPreviewDialog(
            pdfFile = tempPdfFile!!,
            onDismiss = { 
                tempPdfFile?.delete()
                tempPdfFile = null 
            },
            onSave = {
                pdfLauncher.launch("Technical_Sheet_$carName.pdf")
            }
        )
    }

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_technical_sheet)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    Button(
                        onClick = { 
                            val successState = state as? CarDetailsUiState.Success
                            if (successState != null) {
                                scope.launch {
                                    try {
                                        val file = File(context.cacheDir, "temp_report.pdf")
                                        withContext(Dispatchers.IO) {
                                            file.outputStream().use { os ->
                                                PdfReportGenerator.generateReport(
                                                    context = context,
                                                    car = successState.car,
                                                    mileageLogs = successState.mileageLogs,
                                                    inspections = successState.inspections,
                                                    fuelLogs = successState.fuelLogs,
                                                    tireSets = successState.tireSets,
                                                    maintenanceLogs = successState.maintenanceLogs,
                                                    insurances = successState.insurances,
                                                    vignettes = successState.vignettes,
                                                    outputStream = os,
                                                    reportType = PdfReportGenerator.ReportType.TECHNICAL_SHEET
                                                )
                                            }
                                        }
                                        tempPdfFile = file
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.common_save_pdf))
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
                val powerText = CarFormatters.formatPower(context, car)
                
                val country = europeanCountries.find { it.code == car.plateCountry }
                val usesMiles = s.unitSystem == UnitSystem.IMPERIAL
                val speedUnit = if (usesMiles) "mph" else "km/h"
                val displayTopSpeed = CarFormatters.fromCanonicalSpeed(car.topSpeed, usesMiles)
                val topSpeedText = if (displayTopSpeed > 0) "${displayTopSpeed.roundToInt()}\u00A0$speedUnit" else ""
                
                val tireSizeText = if (car.tireWidth > 0 && car.tireAspectRatio > 0 && car.tireDiameter > 0) {
                    "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}"
                } else ""

                val consumptionUnit = if (usesMiles) "MPG" else "L/100km"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = car.displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. TOP QUICK SYMBOLIC STATS GRID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickSpecCard(
                            icon = Icons.Default.Speed,
                            label = "Putere",
                            value = powerText,
                            modifier = Modifier.weight(1f)
                        )
                        QuickSpecCard(
                            icon = Icons.Default.LocalGasStation,
                            label = "Combustibil",
                            value = CarTranslations.getFuelTypeLabel(context, car.fuelType),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickSpecCard(
                            icon = Icons.Default.Settings,
                            label = "Transmisie",
                            value = CarTranslations.getGearboxTypeLabel(context, car.gearboxType),
                            modifier = Modifier.weight(1f)
                        )
                        QuickSpecCard(
                            icon = CheckEngineIcon,
                            label = "Cilindree",
                            value = car.engineSize.ifBlank { "-" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. IDENTITY SECTION
                    TechnicalCategory(
                        title = stringResource(R.string.car_identity_section),
                        icon = Icons.Default.DirectionsCar
                    ) {
                        SpecificationCard(
                            specItems = listOf(
                                SpecItem(stringResource(R.string.car_make_label), car.make, Icons.Default.DirectionsCar),
                                SpecItem(stringResource(R.string.car_model_label), car.model, Icons.Outlined.DirectionsCar),
                                SpecItem(stringResource(R.string.car_generation_label), car.generation, Icons.Default.AutoAwesome),
                                SpecItem(stringResource(R.string.car_year_label), car.year.takeIf { it != 0 }?.toString().orEmpty(), Icons.Default.CalendarToday),
                                SpecItem(stringResource(R.string.car_color_label), CarTranslations.getColorLabel(context, car.color), Icons.Default.Palette),
                                SpecItem(stringResource(R.string.car_license_plate_label), car.licensePlate, Icons.Default.Badge),
                                SpecItem(stringResource(R.string.car_plate_country_label), (country?.let { "${it.flag} ${CarTranslations.getCountryName(context, it.code, it.name)}" } ?: car.plateCountry), Icons.Default.Flag),
                                SpecItem(stringResource(R.string.car_vin_label), car.vin, Icons.Default.Fingerprint),
                                SpecItem(stringResource(R.string.car_vehicle_type_label), CarTranslations.getVehicleTypeLabel(context, car.vehicleType), Icons.Outlined.DirectionsCar),
                                SpecItem(stringResource(R.string.car_manufacturing_country_label), (europeanCountries.find { it.name == car.manufacturingCountry }?.let { "${it.flag} ${CarTranslations.getCountryName(context, it.code, it.name)}" } ?: car.manufacturingCountry), Icons.Default.Public)
                            )
                        )
                    }

                    // 3. ENGINE & TRANSMISSION
                    TechnicalCategory(
                        title = stringResource(R.string.car_engine_transmission_section),
                        icon = CheckEngineIcon
                    ) {
                        SpecificationCard(
                            specItems = listOf(
                                SpecItem(stringResource(R.string.car_engine_size_label), if (car.engineSize.isNotBlank()) context.getString(R.string.formatter_engine_size, car.engineSize) else "", CheckEngineIcon),
                                SpecItem(stringResource(R.string.car_fuel_type_label), CarTranslations.getFuelTypeLabel(context, car.fuelType), Icons.Default.LocalGasStation),
                                SpecItem((if (car.fuelType == "Diesel") stringResource(R.string.car_fuel_system_label) else stringResource(R.string.car_injection_system_label)), CarTranslations.getFuelSystemLabel(context, car.fuelSystem), Icons.Default.WaterDrop),
                                SpecItem(stringResource(R.string.car_aspiration_label), CarTranslations.getAspirationLabel(context, car.aspiration), Icons.Default.Air),
                                SpecItem(stringResource(R.string.car_power_label), powerText, Icons.Default.Speed),
                                SpecItem(stringResource(R.string.car_torque_label), if (car.torque > 0) "${car.torque}\u00A0Nm" else "", Icons.Default.ElectricBolt),
                                SpecItem(stringResource(R.string.car_engine_code_label), car.engineCode, Icons.Default.Code),
                                SpecItem(stringResource(R.string.car_engine_layout_label), CarTranslations.getEngineLayoutLabel(context, car.engineLayout), Icons.Default.Route),
                                SpecItem(stringResource(R.string.car_cylinders_label), car.numberOfCylinders.takeIf { it != 0 }?.toString().orEmpty(), Icons.Default.Compress),
                                SpecItem(stringResource(R.string.car_valves_per_cyl_label), car.valvesPerCylinder.takeIf { it != 0 }?.toString().orEmpty(), Icons.Default.Compress),
                                SpecItem(stringResource(R.string.car_valves_label), (car.numberOfCylinders * car.valvesPerCylinder).takeIf { it > 0 }?.toString().orEmpty(), Icons.Default.Compress),
                                SpecItem(stringResource(R.string.car_cylinder_layout_label), CarTranslations.getCylinderLayoutLabel(context, car.cylinderLayout), Icons.Default.Route),
                                SpecItem(stringResource(R.string.car_acceleration_label), if (car.acceleration0to100 > 0) "${car.acceleration0to100}\u00A0sec" else "", Icons.Default.Timer),
                                SpecItem(stringResource(R.string.car_top_speed_label), topSpeedText, Icons.Default.Speed),
                                SpecItem(stringResource(R.string.car_emission_standard_label), CarTranslations.getEmissionStandardLabel(context, car.emissionStandard), Icons.Default.Eco),
                                SpecItem(stringResource(R.string.car_co2_label), if (car.co2Emissions > 0) "${car.co2Emissions}\u00A0g/km" else "", Icons.Default.Cloud),
                                SpecItem(stringResource(R.string.car_consumption_urban_full), if (car.fuelConsumptionUrban > 0) String.format(
                                    Locale.US, "%.2f %s", CarFormatters.fromCanonicalConsumption(car.fuelConsumptionUrban, usesMiles), consumptionUnit) else "", Icons.Default.LocalGasStation),
                                SpecItem(stringResource(R.string.car_consumption_extra_urban_full), if (car.fuelConsumptionExtraUrban > 0) String.format(
                                    Locale.US, "%.2f %s", CarFormatters.fromCanonicalConsumption(car.fuelConsumptionExtraUrban, usesMiles), consumptionUnit) else "", Icons.Default.LocalGasStation),
                                SpecItem(stringResource(R.string.car_consumption_mixed_full), if (car.fuelConsumptionCombined > 0) String.format(
                                    Locale.US, "%.2f %s", CarFormatters.fromCanonicalConsumption(car.fuelConsumptionCombined, usesMiles), consumptionUnit) else "", Icons.Default.LocalGasStation),
                                SpecItem(stringResource(R.string.car_gearbox_type_label), CarTranslations.getGearboxTypeLabel(context, car.gearboxType), Icons.Default.Settings),
                                SpecItem(stringResource(R.string.car_gears_count_label), car.gears, Icons.Default.FormatListNumbered),
                                SpecItem(stringResource(R.string.car_drivetrain_label), CarTranslations.getDrivetrainLabel(context, car.drivetrain), Icons.Default.Route),
                                SpecItem(stringResource(R.string.car_front_suspension_label), CarTranslations.getSuspensionLabel(context, car.frontSuspension), Icons.Default.LinearScale),
                                SpecItem(stringResource(R.string.car_rear_suspension_label), CarTranslations.getSuspensionLabel(context, car.rearSuspension), Icons.Default.LinearScale),
                                SpecItem(stringResource(R.string.car_front_brakes_label), CarTranslations.getBrakesLabel(context, car.frontBrakes), Icons.Default.DiscFull),
                                SpecItem(stringResource(R.string.car_rear_brakes_label), CarTranslations.getBrakesLabel(context, car.rearBrakes), Icons.Default.DiscFull)
                            )
                        )
                    }

                    // 4. DIMENSIONS & CAPACITIES
                    TechnicalCategory(
                        title = stringResource(R.string.car_dimensions_section),
                        icon = Icons.Default.Straighten
                    ) {
                        val mm = stringResource(R.string.pdf_unit_mm)
                        val dimensionSpecs = mutableListOf(
                            SpecItem(stringResource(R.string.car_tire_size_label), tireSizeText, Icons.Default.TireRepair),
                            SpecItem(stringResource(R.string.car_length_label), if (car.length > 0) "${car.length}\u00A0$mm" else "", Icons.Default.Straighten),
                            SpecItem(stringResource(R.string.car_width_label), if (car.width > 0) "${car.width}\u00A0$mm" else "", Icons.Default.Straighten),
                            SpecItem(stringResource(R.string.car_height_label), if (car.height > 0) "${car.height}\u00A0$mm" else "", Icons.Default.Straighten),
                            SpecItem(stringResource(R.string.car_wheelbase_label), if (car.wheelbase > 0) "${car.wheelbase}\u00A0$mm" else "", Icons.Default.Straighten),
                            SpecItem(stringResource(R.string.car_weight_label), if (car.weight > 0) "${car.weight}\u00A0kg" else "", Icons.Default.Scale),
                            SpecItem(stringResource(R.string.car_boot_label), if (car.bootSpace > 0) "${car.bootSpace}\u00A0L" else "", Icons.Default.Luggage),
                            SpecItem(stringResource(R.string.car_seats_label), car.numberOfSeats.takeIf { it != 0 }?.toString().orEmpty(), Icons.Default.EventSeat),
                            SpecItem(stringResource(R.string.car_doors_label), car.numberOfDoors.takeIf { it != 0 }?.toString().orEmpty(), Icons.Default.DoorSliding)
                        )

                        if (car.fuelType != "Electric" && car.fuelTankCapacity > 0) {
                            dimensionSpecs.add(SpecItem(stringResource(R.string.car_fuel_tank_label), "${car.fuelTankCapacity}\u00A0L", Icons.Default.EvStation))
                        }
                        if ((car.fuelType == "Electric" || car.fuelType == "Hybrid") && car.batteryCapacity > 0) {
                            dimensionSpecs.add(SpecItem(stringResource(R.string.car_battery_capacity_label), "${car.batteryCapacity}\u00A0kWh", Icons.Default.BatteryChargingFull))
                        }

                        SpecificationCard(specItems = dimensionSpecs)
                    }

                    // 5. SAFETY & EQUIPMENTS
                    val safetyIds = listOf(
                        CarEquipment.ABS,
                        CarEquipment.ESP,
                        CarEquipment.ASR,
                        CarEquipment.ISOFIX
                    )

                    TechnicalCategory(
                        title = stringResource(R.string.car_safety_section),
                        icon = Icons.Default.Security
                    ) {
                        val safetyEquipments = car.equipments.filter { it in safetyIds }
                        EquipmentBadgesList(equipments = safetyEquipments, context = context)
                    }

                    TechnicalCategory(
                        title = stringResource(R.string.car_equipments_label),
                        icon = Icons.Default.CheckCircle
                    ) {
                        val otherEquipments = car.equipments.filter { it !in safetyIds }
                        EquipmentBadgesList(equipments = otherEquipments, context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSpecCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = Color.White
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value.ifBlank { "-" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentBadgesList(
    equipments: List<String>,
    context: Context
) {
    if (equipments.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Text(
                text = stringResource(R.string.common_none),
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            equipments.forEach { id ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = CarTranslations.getEquipmentLabel(context, id),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicalCategory(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}
