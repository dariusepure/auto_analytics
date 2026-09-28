package com.dariusepure.caractivitylog.ui.cars

import com.dariusepure.caractivitylog.data.ai.GeminiRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.ScannedCarData
import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt
import com.dariusepure.caractivitylog.R

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository,
    private val geminiRepository: GeminiRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<AddCarState>(AddCarState.Idle)
    val state = _state.asStateFlow()

    val unitSystem = preferenceRepository.unitSystem

    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _scannedDataEvent = Channel<List<ScannedCarData>>(Channel.BUFFERED)
    val scannedDataEvent = _scannedDataEvent.receiveAsFlow()

    private val _logoutEvent = Channel<Unit>(Channel.BUFFERED)
    val logoutEvent = _logoutEvent.receiveAsFlow()

    private var currentCarId: String? = null

    fun loadCar(carId: String) {
        currentCarId = carId
        viewModelScope.launch {
            _state.value = AddCarState.Pending
            try {
                val car = carRepository.getCar(carId)
                if (car != null) {
                    _state.value = AddCarState.Idle
                }
            } catch (e: Exception) {
                _state.value = AddCarState.Error(e.localizedMessage ?: context.getString(R.string.error_failed_to_load_car))
            }
        }
    }

    suspend fun getCarData(carId: String): Car? = carRepository.getCar(carId)

    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = AddCarState.Scanning
            geminiRepository.scanRegistrationCertificate(bitmap)
                .onSuccess { data ->
                    _state.value = AddCarState.Idle
                    _scannedDataEvent.trySend(data)
                }
                .onFailure { e ->
                    val message = when {
                        e.message?.contains("403") == true || e.message?.contains("PERMISSION_DENIED") == true -> {
                            context.getString(R.string.error_ai_scan_api_key)
                        }
                        else -> e.localizedMessage ?: context.getString(R.string.error_ai_scan_failed)
                    }
                    _state.value = AddCarState.Error(message)
                }
        }
    }

    fun scanDocument(uri: Uri, mimeType: String) {
        viewModelScope.launch {
            _state.value = AddCarState.Scanning
            geminiRepository.scanDocument(uri, mimeType)
                .onSuccess { data ->
                    _state.value = AddCarState.Idle
                    _scannedDataEvent.trySend(data)
                }
                .onFailure { e ->
                    val message = when {
                        e.message?.contains("403") == true || e.message?.contains("PERMISSION_DENIED") == true -> {
                            context.getString(R.string.error_ai_scan_api_key)
                        }
                        else -> e.localizedMessage ?: context.getString(R.string.error_ai_scan_failed)
                    }
                    _state.value = AddCarState.Error(message)
                }
        }
    }

    fun onAddOrUpdateCar(
        licensePlate: String,
        plateCountry: String,
        make: String,
        model: String,
        generation: String,
        vin: String,
        year: String,
        engineSize: String,
        fuelType: String,
        fuelSystem: String,
        color: String,
        power: String,
        powerUnit: String,
        torque: String,
        engineCode: String,
        engineLayout: String,
        cylinderLayout: String,
        emissionStandard: String,
        length: String,
        width: String,
        height: String,
        wheelbase: String,
        fuelTankCapacity: String,
        batteryCapacity: String,
        drivetrain: String,
        gearboxType: String,
        gears: String,
        frontSuspension: String,
        rearSuspension: String,
        aspiration: String,
        frontBrakes: String,
        rearBrakes: String,
        vehicleType: String,
        manufacturingCountry: String,
        topSpeed: String,
        weight: String,
        numberOfSeats: String,
        numberOfCylinders: String,
        valvesPerCylinder: String,
        numberOfDoors: String,
        bootSpace: String,
        tireWidth: String,
        tireAspectRatio: String,
        tireDiameter: String,
        engineVariant: String = "",
        acceleration0to100: String = "",
        fuelConsumptionCombined: String = "",
        fuelConsumptionUrban: String = "",
        fuelConsumptionExtraUrban: String = "",
        co2Emissions: String = "",
        airbags: String = "",
        equipments: List<String> = emptyList()
    ) {
        _state.value = AddCarState.Pending

        if (make.isBlank() || model.isBlank()) {
            _state.value = AddCarState.Error(context.getString(R.string.validation_brand_model_required))
            return
        }

        val numericFields = mapOf(
            context.getString(R.string.car_year_label) to year,
            context.getString(R.string.car_power_label) to power,
            context.getString(R.string.car_torque_label) to torque,
            context.getString(R.string.car_length_label) to length,
            context.getString(R.string.car_width_label) to width,
            context.getString(R.string.car_height_label) to height,
            context.getString(R.string.car_wheelbase_label) to wheelbase,
            context.getString(R.string.car_fuel_tank_capacity_label) to fuelTankCapacity,
            context.getString(R.string.car_battery_capacity_label) to batteryCapacity,
            context.getString(R.string.car_top_speed_label) to topSpeed,
            context.getString(R.string.car_weight_label) to weight,
            context.getString(R.string.car_seats_label) to numberOfSeats,
            context.getString(R.string.car_cylinders_label) to numberOfCylinders,
            context.getString(R.string.car_valves_per_cyl_label) to valvesPerCylinder,
            context.getString(R.string.car_doors_label) to numberOfDoors,
            context.getString(R.string.car_boot_label) to bootSpace,
            context.getString(R.string.car_tire_width_label) to tireWidth,
            context.getString(R.string.car_tire_ratio_label) to tireAspectRatio,
            context.getString(R.string.car_tire_diam_label) to tireDiameter,
            context.getString(R.string.car_acceleration_label) to acceleration0to100,
            context.getString(R.string.car_consumption_label) to fuelConsumptionCombined,
            context.getString(R.string.car_consumption_urban_label) to fuelConsumptionUrban,
            context.getString(R.string.car_consumption_extra_urban_label) to fuelConsumptionExtraUrban,
            context.getString(R.string.car_co2_label) to co2Emissions,
            context.getString(R.string.car_airbags_label) to airbags
        )

        for ((label, value) in numericFields) {
            val clean = value.trim().replace(",", ".")
            if (clean.isNotBlank()) {
                val dValue = clean.toDoubleOrNull()
                if (dValue == null) {
                    _state.value = AddCarState.Error(context.getString(R.string.validation_numeric_format, label))
                    return
                }
                if (dValue < 0) {
                    _state.value = AddCarState.Error(context.getString(R.string.validation_negative_value))
                    return
                }
            }
        }

        viewModelScope.launch {
            _state.value = AddCarState.Pending
            try {
                // Duplicate VIN Check
                if (vin.isNotBlank()) {
                    val isDuplicate = carRepository.isVinDuplicate(vin.trim(), currentCarId)
                    if (isDuplicate) {
                        _state.value = AddCarState.Error(context.getString(R.string.validation_vin_exists))
                        return@launch
                    }
                }

                val unitSystemValue = preferenceRepository.unitSystem.first()
                val usesMiles = unitSystemValue == com.dariusepure.caractivitylog.domain.UnitSystem.IMPERIAL
                
                fun parseDouble(s: String) = s.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                fun parseInt(s: String) = s.trim().replace(",", ".").toDoubleOrNull()?.roundToInt() ?: 0

                val inputTopSpeed = parseDouble(topSpeed)
                val canonicalTopSpeed = CarFormatters.toCanonicalSpeed(inputTopSpeed, usesMiles)

                val car = Car(
                    id = currentCarId ?: "",
                    name = "",
                    licensePlate = licensePlate.uppercase(),
                    plateCountry = plateCountry,
                    make = make.trim().lowercase().replaceFirstChar { it.uppercase() },
                    model = model.trim(),
                    generation = generation.trim(),
                    vin = vin.trim().uppercase(),
                    year = parseInt(year),
                    engineSize = engineSize.trim(),
                    fuelType = fuelType.trim(),
                    fuelSystem = fuelSystem.trim(),
                    color = color.trim(),
                    power = parseInt(power),
                    powerUnit = powerUnit,
                    torque = parseInt(torque),
                    engineCode = engineCode.trim(),
                    engineLayout = engineLayout.trim(),
                    cylinderLayout = cylinderLayout.trim(),
                    emissionStandard = emissionStandard.trim(),
                    aspiration = aspiration.trim(),
                    length = parseInt(length),
                    width = parseInt(width),
                    height = parseInt(height),
                    wheelbase = parseInt(wheelbase),
                    fuelTankCapacity = parseDouble(fuelTankCapacity),
                    batteryCapacity = parseDouble(batteryCapacity),
                    drivetrain = drivetrain.trim(),
                    gearboxType = gearboxType.trim(),
                    gears = gears.trim(),
                    frontSuspension = frontSuspension.trim(),
                    rearSuspension = rearSuspension.trim(),
                    frontBrakes = frontBrakes.trim(),
                    rearBrakes = rearBrakes.trim(),
                    vehicleType = vehicleType.trim(),
                    manufacturingCountry = manufacturingCountry.trim(),
                    topSpeed = canonicalTopSpeed,
                    weight = parseInt(weight),
                    numberOfSeats = parseInt(numberOfSeats),
                    numberOfCylinders = parseInt(numberOfCylinders),
                    valvesPerCylinder = parseInt(valvesPerCylinder),
                    numberOfDoors = parseInt(numberOfDoors),
                    bootSpace = parseInt(bootSpace),
                    tireWidth = parseInt(tireWidth),
                    tireAspectRatio = parseInt(tireAspectRatio),
                    tireDiameter = parseInt(tireDiameter),
                    acceleration0to100 = parseDouble(acceleration0to100),
                    fuelConsumptionCombined = CarFormatters.toCanonicalConsumption(parseDouble(fuelConsumptionCombined), usesMiles),
                    fuelConsumptionUrban = CarFormatters.toCanonicalConsumption(parseDouble(fuelConsumptionUrban), usesMiles),
                    fuelConsumptionExtraUrban = CarFormatters.toCanonicalConsumption(parseDouble(fuelConsumptionExtraUrban), usesMiles),
                    co2Emissions = parseInt(co2Emissions),
                    updatedAt = Date(),
                    airbags = parseInt(airbags),
                    equipments = equipments,
                    engineVariant = engineVariant.trim()
                )

                carRepository.createCar(car)
                _state.value = AddCarState.Success
                _navigationEvent.trySend(Unit)

            } catch (e: Exception) {
                _state.value = AddCarState.Error(e.localizedMessage ?: context.getString(R.string.error_failed_to_save_car))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _logoutEvent.trySend(Unit)
            } catch (e: Exception) {
                _state.value = AddCarState.Error(e.localizedMessage ?: context.getString(R.string.error_failed_to_sign_out))
            }
        }
    }

    fun resetState() {
        _state.value = AddCarState.Idle
        currentCarId = null
    }
}
