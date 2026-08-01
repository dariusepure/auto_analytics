package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import com.dariusepure.caractivitylog.ui.common.CarTranslations
import com.dariusepure.caractivitylog.ui.common.ErrorState
import com.dariusepure.caractivitylog.ui.common.LoadingState
import com.dariusepure.caractivitylog.ui.common.SpecificationCard
import com.dariusepure.caractivitylog.domain.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalSheetScreen(
    carId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_technical_sheet)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                val totalValves = car.numberOfCylinders * car.valvesPerCylinder
                val valvesText = if (totalValves > 0) {
                    stringResource(R.string.formatter_valves_total, totalValves, car.valvesPerCylinder)
                } else "-"
                
                val country = europeanCountries.find { it.code == car.plateCountry }
                val usesMiles = country?.usesMiles == true
                val speedUnit = if (usesMiles) "mph" else "km/h"
                val displayTopSpeed = CarFormatters.fromCanonicalSpeed(car.topSpeed, usesMiles)
                val topSpeedText = if (displayTopSpeed > 0) "${displayTopSpeed.roundToInt()}\u00A0$speedUnit" else "-"
                
                val tireSizeText = if (car.tireWidth > 0 && car.tireAspectRatio > 0 && car.tireDiameter > 0) {
                    "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}"
                } else "-"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = car.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    TechnicalCategory(title = stringResource(R.string.car_identity_section)) {
                        SpecificationCard(
                            specifications = listOf(
                                stringResource(R.string.car_make_label) to car.make,
                                stringResource(R.string.car_model_label) to car.model,
                                stringResource(R.string.car_vehicle_type_label) to CarTranslations.getVehicleTypeLabel(context, car.vehicleType),
                                stringResource(R.string.car_year_label) to car.year.takeIf { it != 0 }?.toString().orEmpty(),
                                stringResource(R.string.car_color_label) to car.color,
                                stringResource(R.string.car_license_plate_label) to car.licensePlate,
                                stringResource(R.string.car_plate_country_label) to (country?.let { "${it.flag} ${it.name}" } ?: car.plateCountry),
                                stringResource(R.string.car_vin_label) to car.vin,
                                stringResource(R.string.car_manufacturing_country_label) to (europeanCountries.find { it.name == car.manufacturingCountry }?.let { "${it.flag} ${it.name}" } ?: car.manufacturingCountry)
                            )
                        )
                    }

                    TechnicalCategory(title = stringResource(R.string.car_engine_section)) {
                        SpecificationCard(
                            specifications = listOf(
                                stringResource(R.string.car_power_label) to powerText,
                                stringResource(R.string.car_torque_label) to if (car.torque > 0) "${car.torque}\u00A0Nm" else "",
                                stringResource(R.string.car_top_speed_label) to topSpeedText,
                                stringResource(R.string.car_aspiration_label) to CarTranslations.getAspirationLabel(context, car.aspiration),
                                stringResource(R.string.car_cylinders_label) to car.numberOfCylinders.takeIf { it != 0 }?.toString().orEmpty(),
                                stringResource(R.string.car_valves_label) to valvesText,
                                stringResource(R.string.car_engine_size_label) to if (car.engineSize.isNotBlank()) "${car.engineSize}\u00A0cc" else "",
                                stringResource(R.string.car_fuel_type_label) to CarTranslations.getFuelTypeLabel(context, car.fuelType),
                                stringResource(R.string.car_injection_system_label) to CarTranslations.getFuelSystemLabel(context, car.fuelSystem),
                                stringResource(R.string.car_engine_code_label) to car.engineCode,
                                stringResource(R.string.car_engine_layout_label) to CarTranslations.getEngineLayoutLabel(context, car.engineLayout),
                                stringResource(R.string.car_cylinder_layout_label) to CarTranslations.getCylinderLayoutLabel(context, car.cylinderLayout),
                                stringResource(R.string.car_emission_standard_label) to CarTranslations.getEmissionStandardLabel(context, car.emissionStandard)
                            )
                        )
                    }

                    TechnicalCategory(title = stringResource(R.string.car_transmission_section)) {
                        SpecificationCard(
                            specifications = listOf(
                                stringResource(R.string.car_gearbox_type_label) to CarTranslations.getGearboxTypeLabel(context, car.gearboxType),
                                stringResource(R.string.car_gears_count_label) to car.gears,
                                stringResource(R.string.car_drivetrain_label) to CarTranslations.getDrivetrainLabel(context, car.drivetrain),
                                stringResource(R.string.car_front_suspension_label) to CarTranslations.getSuspensionLabel(context, car.frontSuspension),
                                stringResource(R.string.car_rear_suspension_label) to CarTranslations.getSuspensionLabel(context, car.rearSuspension),
                                stringResource(R.string.car_front_brakes_label) to CarTranslations.getBrakesLabel(context, car.frontBrakes),
                                stringResource(R.string.car_rear_brakes_label) to CarTranslations.getBrakesLabel(context, car.rearBrakes)
                            )
                        )
                    }

                    TechnicalCategory(title = stringResource(R.string.car_dimensions_capacity_section)) {
                        val dimensionSpecs = mutableListOf(
                            stringResource(R.string.car_dimensions_lxwxh) to CarFormatters.formatDimensions(context, car),
                            stringResource(R.string.car_wheelbase_label) to if (car.wheelbase > 0) "${car.wheelbase}\u00A0mm" else "",
                            stringResource(R.string.car_track_width_label) to if (car.trackWidth > 0) "${car.trackWidth}\u00A0mm" else "",
                            stringResource(R.string.car_weight_label) to if (car.weight > 0) "${car.weight}\u00A0kg" else "",
                            stringResource(R.string.car_seats_label) to car.numberOfSeats.takeIf { it != 0 }?.toString().orEmpty(),
                            stringResource(R.string.car_doors_label) to car.numberOfDoors.takeIf { it != 0 }?.toString().orEmpty(),
                            stringResource(R.string.car_boot_label) to if (car.bootSpace > 0) "${car.bootSpace}\u00A0L" else ""
                        )

                        if (car.fuelType != "Electric") {
                            dimensionSpecs.add(stringResource(R.string.car_fuel_tank_label) to if (car.fuelTankCapacity > 0) "${car.fuelTankCapacity}\u00A0L" else "")
                        }
                        if (car.fuelType == "Electric" || car.fuelType == "Hybrid") {
                            dimensionSpecs.add(stringResource(R.string.car_battery_capacity_label) to if (car.batteryCapacity > 0) "${car.batteryCapacity}\u00A0kWh" else "")
                        }

                        dimensionSpecs.add(stringResource(R.string.car_tire_size_label) to tireSizeText)

                        SpecificationCard(specifications = dimensionSpecs)
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicalCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
