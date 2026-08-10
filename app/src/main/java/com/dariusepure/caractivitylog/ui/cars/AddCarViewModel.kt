package com.dariusepure.caractivitylog.ui.cars

import com.dariusepure.caractivitylog.data.ai.GeminiRepository
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AddCarState>(AddCarState.Idle)
    val state = _state.asStateFlow()

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
                _state.value = AddCarState.Error(e.localizedMessage ?: "Failed to load car")
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
                            "AI Scan failed: Your API key is invalid or leaked. Please update it."
                        }
                        else -> e.localizedMessage ?: "AI Scan failed"
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
                            "AI Scan failed: Your API key is invalid or leaked. Please update it."
                        }
                        else -> e.localizedMessage ?: "AI Scan failed"
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
        acceleration0to100: String = "",
        fuelConsumptionCombined: String = "",
        fuelConsumptionUrban: String = "",
        fuelConsumptionExtraUrban: String = "",
        co2Emissions: String = ""
    ) {
        if (make.isBlank() || model.isBlank()) {
            _state.value = AddCarState.Error("Please provide Brand & Model")
            return
        }

        // VIN validation: empty is okay (optional), but if not empty must be 17 chars
        if (vin.isNotBlank() && vin.length != 17) {
            _state.value = AddCarState.Error("VIN must be exactly 17 characters if provided")
            return
        }

        // Numeric validations to prevent negative values
        val numericFields = mapOf(
            "Year" to year,
            "Power" to power,
            "Torque" to torque,
            "Length" to length,
            "Width" to width,
            "Height" to height,
            "Wheelbase" to wheelbase,
            "Fuel Tank Capacity" to fuelTankCapacity,
            "Battery Capacity" to batteryCapacity,
            "Top Speed" to topSpeed,
            "Weight" to weight,
            "Seats" to numberOfSeats,
            "Cylinders" to numberOfCylinders,
            "Valves/Cyl" to valvesPerCylinder,
            "Doors" to numberOfDoors,
            "Boot Space" to bootSpace,
            "Tire Width" to tireWidth,
            "Tire Ratio" to tireAspectRatio,
            "Tire Diameter" to tireDiameter,
            "Acceleration" to acceleration0to100,
            "Consumption Mixed" to fuelConsumptionCombined,
            "Consumption Urban" to fuelConsumptionUrban,
            "Consumption Extra-Urban" to fuelConsumptionExtraUrban,
            "CO2" to co2Emissions
        )

        for ((label, value) in numericFields) {
            if (value.isNotBlank()) {
                val dValue = value.toDoubleOrNull()
                if (dValue == null) {
                    _state.value = AddCarState.Error("Invalid numeric format for $label")
                    return
                }
                if (dValue < 0) {
                    _state.value = AddCarState.Error("$label cannot be negative")
                    return
                }
            }
        }

        viewModelScope.launch {
            _state.value = AddCarState.Pending
            try {
                val country = europeanCountries.find { it.code == plateCountry }
                val usesMiles = country?.usesMiles == true
                
                val inputTopSpeed = topSpeed.toDoubleOrNull() ?: 0.0
                val canonicalTopSpeed = CarFormatters.toCanonicalSpeed(inputTopSpeed, usesMiles)

                var finalPower = power.toDoubleOrNull()?.roundToInt() ?: 0
                // If country changed and units differ, we could convert power too? 
                // But request was specifically for mileage conversion and country selection.

                val car = Car(
                    id = currentCarId ?: "",
                    name = "",
                    licensePlate = licensePlate.uppercase(),
                    plateCountry = plateCountry,
                    make = make.trim().uppercase(),
                    model = model.trim(),
                    vin = vin.trim().uppercase(),
                    year = year.toDoubleOrNull()?.roundToInt() ?: 0,
                    engineSize = engineSize,
                    fuelType = fuelType,
                    fuelSystem = fuelSystem,
                    color = color,
                    power = finalPower,
                    powerUnit = powerUnit,
                    torque = torque.toDoubleOrNull()?.roundToInt() ?: 0,
                    engineCode = engineCode,
                    engineLayout = engineLayout,
                    cylinderLayout = cylinderLayout,
                    emissionStandard = emissionStandard,
                    aspiration = aspiration,
                    length = length.toDoubleOrNull()?.roundToInt() ?: 0,
                    width = width.toDoubleOrNull()?.roundToInt() ?: 0,
                    height = height.toDoubleOrNull()?.roundToInt() ?: 0,
                    wheelbase = wheelbase.toDoubleOrNull()?.roundToInt() ?: 0,
                    fuelTankCapacity = fuelTankCapacity.toDoubleOrNull() ?: 0.0,
                    batteryCapacity = batteryCapacity.toDoubleOrNull() ?: 0.0,
                    drivetrain = drivetrain,
                    gearboxType = gearboxType,
                    gears = gears,
                    frontSuspension = frontSuspension,
                    rearSuspension = rearSuspension,
                    frontBrakes = frontBrakes,
                    rearBrakes = rearBrakes,
                    vehicleType = vehicleType,
                    manufacturingCountry = manufacturingCountry,
                    topSpeed = canonicalTopSpeed,
                    weight = weight.toDoubleOrNull()?.roundToInt() ?: 0,
                    numberOfSeats = numberOfSeats.toDoubleOrNull()?.roundToInt() ?: 0,
                    numberOfCylinders = numberOfCylinders.toDoubleOrNull()?.roundToInt() ?: 0,
                    valvesPerCylinder = valvesPerCylinder.toDoubleOrNull()?.roundToInt() ?: 0,
                    numberOfDoors = numberOfDoors.toDoubleOrNull()?.roundToInt() ?: 0,
                    bootSpace = bootSpace.toDoubleOrNull()?.roundToInt() ?: 0,
                    tireWidth = tireWidth.toDoubleOrNull()?.roundToInt() ?: 0,
                    tireAspectRatio = tireAspectRatio.toDoubleOrNull()?.roundToInt() ?: 0,
                    tireDiameter = tireDiameter.toDoubleOrNull()?.roundToInt() ?: 0,
                    acceleration0to100 = acceleration0to100.toDoubleOrNull() ?: 0.0,
                    fuelConsumptionCombined = fuelConsumptionCombined.toDoubleOrNull() ?: 0.0,
                    fuelConsumptionUrban = fuelConsumptionUrban.toDoubleOrNull() ?: 0.0,
                    fuelConsumptionExtraUrban = fuelConsumptionExtraUrban.toDoubleOrNull() ?: 0.0,
                    co2Emissions = co2Emissions.toDoubleOrNull()?.roundToInt() ?: 0,
                    updatedAt = Date()
                )

                carRepository.createCar(car)
                _state.value = AddCarState.Success

                // 2. Folosim trySend() care trimite instant semnalul de back și deblochează ecranul
                _navigationEvent.trySend(Unit)

            } catch (e: Exception) {
                _state.value = AddCarState.Error(e.localizedMessage ?: "Failed to save car")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _logoutEvent.trySend(Unit)
            } catch (e: Exception) {
                _state.value = AddCarState.Error(e.localizedMessage ?: "Failed to sign out")
            }
        }
    }

    fun resetState() {
        _state.value = AddCarState.Idle
        currentCarId = null
    }
}
