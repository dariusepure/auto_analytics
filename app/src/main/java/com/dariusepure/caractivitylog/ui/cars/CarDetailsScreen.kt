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
                            carAccentColor = carAccentColor
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // Menu Items - One per row
                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_technical_sheet),
                            icon = Icons.Default.Description,
                            onClick = onTechnicalSheetClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_mileage_history),
                            icon = Icons.Default.Speed,
                            secondaryIcon = Icons.Default.Add,
                            onClick = onMileageClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_inspection_title),
                            icon = Icons.Default.AssignmentTurnedIn,
                            onClick = onInspectionClick,
                            containerColor = inspectionColor.copy(alpha = 0.15f),
                            contentColor = inspectionColor,
                            badgeLabel = stringResource(inspectionLabelRes),
                            badgeColor = inspectionColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_insurance_title),
                            icon = Icons.Default.Security,
                            onClick = onInsuranceClick,
                            containerColor = insuranceColor.copy(alpha = 0.15f),
                            contentColor = insuranceColor,
                            badgeLabel = stringResource(insuranceLabelRes),
                            badgeColor = insuranceColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_vignette_title),
                            icon = Icons.Default.ConfirmationNumber,
                            onClick = onVignetteClick,
                            containerColor = vignetteColor.copy(alpha = 0.15f),
                            contentColor = vignetteColor,
                            badgeLabel = stringResource(vignetteLabelRes),
                            badgeColor = vignetteColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_fuel_consumption),
                            icon = Icons.Default.LocalGasStation,
                            onClick = onFuelClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.service_history_title),
                            icon = Icons.Default.Build,
                            onClick = onServiceClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.tire_management_title),
                            icon = Icons.Default.TireRepair,
                            onClick = onTireClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item {
                        CarDetailRowItem(
                            title = stringResource(R.string.car_diagnosis_title),
                            icon = Icons.Default.Engineering,
                            onClick = onDiagnosisClick,
                            containerColor = carAccentColor.copy(alpha = 0.15f),
                            contentColor = carAccentColor
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CarDetailRowItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    badgeLabel: String? = null,
    badgeColor: Color = Color.Unspecified,
    secondaryIcon: ImageVector? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = contentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    if (secondaryIcon != null) {
                        Icon(
                            imageVector = secondaryIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badgeLabel != null && badgeColor != Color.Unspecified) {
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(
                        label = badgeLabel,
                        color = badgeColor
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CarHeaderHeroCard(
    car: Car,
    carAccentColor: Color,
    modifier: Modifier = Modifier
) {
    val logoRes = remember(car.make) { CarFormatters.getBrandLogoResource(car.make) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Brand Logo Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            modifier = Modifier.size(64.dp)
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
                        modifier = Modifier.size(36.dp),
                        tint = carAccentColor
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = car.displayName,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (car.licensePlate.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            LicensePlateBadge(
                licensePlate = car.licensePlate,
                countryCode = car.plateCountry
            )
        }
    }
}
