package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.domain.*
import com.dariusepure.caractivitylog.ui.common.AutoSizeText
import com.dariusepure.caractivitylog.ui.common.*
import com.dariusepure.caractivitylog.ui.theme.statusExpiredRed
import java.util.*

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

    val carAccentColor = Color(0xFF1A73E8)

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

                val latestInspection = remember(s.inspections) { s.inspections.maxByOrNull { it.date } }
                val latestInsurance = remember(s.insurances) { s.insurances.maxByOrNull { it.date } }
                val latestVignette = remember(s.vignettes) { s.vignettes.maxByOrNull { it.date } }

                val (inspectionColor, inspectionLabelRes) = remember(latestInspection) {
                    val expiryDate = latestInspection?.expiryDate
                    if (expiryDate == null) return@remember Color(0xFF1A73E8) to R.string.common_not_applicable
                    val now = Date()
                    val diff = expiryDate.time - now.time
                    val days = diff / (1000 * 60 * 60 * 24)

                    when {
                        expiryDate.before(now) -> statusExpiredRed to R.string.status_expired
                        days < 14 -> Color(0xFFFF9800) to R.string.status_soon
                        else -> Color(0xFF4CAF50) to R.string.status_ok
                    }
                }

                val (insuranceColor, insuranceLabelRes) = remember(latestInsurance) {
                    val expiryDate = latestInsurance?.expiryDate
                    if (expiryDate == null) return@remember Color(0xFF1A73E8) to R.string.common_not_applicable
                    val now = Date()
                    val diff = expiryDate.time - now.time
                    val days = diff / (1000 * 60 * 60 * 24)

                    when {
                        expiryDate.before(now) -> statusExpiredRed to R.string.status_expired
                        days < 14 -> Color(0xFFFF9800) to R.string.status_soon
                        else -> Color(0xFF4CAF50) to R.string.status_ok
                    }
                }

                val (vignetteColor, vignetteLabelRes) = remember(latestVignette) {
                    val expiryDate = latestVignette?.expiryDate
                    if (expiryDate == null) return@remember Color(0xFF1A73E8) to R.string.common_not_applicable
                    val now = Date()
                    val diff = expiryDate.time - now.time
                    val days = diff / (1000 * 60 * 60 * 24)

                    when {
                        expiryDate.before(now) -> statusExpiredRed to R.string.status_expired
                        days < 14 -> Color(0xFFFF9800) to R.string.status_soon
                        else -> Color(0xFF4CAF50) to R.string.status_ok
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
                        Spacer(Modifier.height(4.dp))
                        CarHeaderHeroCard(
                            car = car,
                            carAccentColor = carAccentColor,
                            context = context
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // Bento Rows
                    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

                    if (isExpanded) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = carAccentColor
                                    )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Speed,
                                            null,
                                            modifier = Modifier.size(48.dp),
                                            tint = carAccentColor
                                        )
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = carAccentColor
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_mileage_history),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = inspectionColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AssignmentTurnedIn,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = inspectionColor
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_inspection_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(inspectionLabelRes),
                                        color = inspectionColor
                                    )
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = insuranceColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = insuranceColor
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_insurance_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(insuranceLabelRes),
                                        color = insuranceColor
                                    )
                                }
                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = vignetteColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ConfirmationNumber,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = vignetteColor
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.car_vignette_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(vignetteLabelRes),
                                        color = vignetteColor
                                    )
                                }
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.LocalGasStation,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = carAccentColor
                                    )
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Build,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = carAccentColor
                                    )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.TireRepair,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = carAccentColor
                                    )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Engineering,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = carAccentColor
                                    )
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
                    } else {
                        // Bento Row 1: Technical & Mileage
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onTechnicalSheetClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        null,
                                        modifier = Modifier.size(52.dp),
                                        tint = carAccentColor
                                    )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Speed,
                                            null,
                                            modifier = Modifier.size(52.dp),
                                            tint = carAccentColor
                                        )
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            modifier = Modifier.size(24.dp),
                                            tint = carAccentColor
                                        )
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onInspectionClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = inspectionColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AssignmentTurnedIn,
                                        null,
                                        modifier = Modifier.size(56.dp),
                                        tint = inspectionColor
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_inspection_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(inspectionLabelRes),
                                        color = inspectionColor
                                    )
                                }

                                BentoCard(
                                    onClick = onInsuranceClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = insuranceColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        null,
                                        modifier = Modifier.size(56.dp),
                                        tint = insuranceColor
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_insurance_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(insuranceLabelRes),
                                        color = insuranceColor
                                    )
                                }

                                BentoCard(
                                    onClick = onVignetteClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = vignetteColor.copy(alpha = 0.15f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ConfirmationNumber,
                                        null,
                                        modifier = Modifier.size(56.dp),
                                        tint = vignetteColor
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.car_vignette_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        softWrap = true,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    StatusBadge(
                                        label = stringResource(vignetteLabelRes),
                                        color = vignetteColor
                                    )
                                }
                            }
                        }

                        // Bento Row 3: Fuel & Tires
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onFuelClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.LocalGasStation,
                                        null,
                                        modifier = Modifier.size(52.dp),
                                        tint = carAccentColor
                                    )
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.car_fuel_consumption),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }

                                BentoCard(
                                    onClick = onTireClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.TireRepair,
                                        null,
                                        modifier = Modifier.size(52.dp),
                                        tint = carAccentColor
                                    )
                                    Spacer(Modifier.weight(1f))
                                    AutoSizeText(
                                        text = stringResource(R.string.tire_management_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minFontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Bento Row 4: Service & Diagnosis
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoCard(
                                    onClick = onServiceClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Build,
                                        null,
                                        modifier = Modifier.size(52.dp),
                                        tint = carAccentColor
                                    )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = carAccentColor.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.Engineering,
                                        null,
                                        modifier = Modifier.size(52.dp),
                                        tint = carAccentColor
                                    )
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
private fun CarHeaderHeroCard(
    car: Car,
    carAccentColor: Color,
    context: Context,
    modifier: Modifier = Modifier
) {
    val logoRes = remember(car.make) { CarFormatters.getBrandLogoResource(car.make) }

    val specPills = remember(car, context) {
        val list = mutableListOf<Pair<ImageVector, String>>()
        if (car.year > 0) {
            list.add(Pair(Icons.Default.CalendarToday, car.year.toString()))
        }
        if (car.vehicleType.isNotBlank()) {
            list.add(Pair(Icons.Outlined.DirectionsCar, CarTranslations.getVehicleTypeLabel(context, car.vehicleType)))
        }
        if (car.fuelType.isNotBlank()) {
            list.add(Pair(Icons.Default.LocalGasStation, CarTranslations.getFuelTypeLabel(context, car.fuelType)))
        }
        if (car.power > 0) {
            list.add(Pair(Icons.Default.Speed, CarFormatters.formatPower(context, car)))
        }
        list
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = carAccentColor.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, carAccentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brand Logo Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        if (logoRes != null) {
                            Image(
                                painter = painterResource(logoRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = carAccentColor
                            )
                        }
                    }
                }

                // Title & License Plate Badge
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = car.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (car.licensePlate.isNotBlank()) {
                        LicensePlateBadge(
                            licensePlate = car.licensePlate,
                            countryCode = car.plateCountry
                        )
                    }

                    if (car.vin.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "VIN: ${car.vin}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Spec Pill Badges Flow Row
            if (specPills.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    specPills.forEach { (icon, text) ->
                        SpecChip(icon = icon, text = text)
                    }
                }
            }
        }
    }
}
