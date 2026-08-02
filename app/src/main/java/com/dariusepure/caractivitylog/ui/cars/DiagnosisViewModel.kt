package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.ai.GeminiRepository
import com.dariusepure.caractivitylog.data.ai.text
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosisUiState())
    val state = _state.asStateFlow()

    private var carContext = ""
    private var currentCarId: String? = null
    private var currentCar: Car? = null

    fun loadCarData(carId: String) {
        currentCarId = carId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val car = carRepository.getCar(carId)
            currentCar = car
            if (car != null) {
                val mileageLogs = carRepository.getMileageLogs(carId).first()
                val latestKm = mileageLogs.maxByOrNull { it.date }?.km ?: 0.0
                
                carContext = """
                    Car: ${car.displayName}, Year: ${car.year}, Engine: ${car.engineSize} ${car.fuelType}
                    Specs: Power: ${car.power}${car.powerUnit}, Torque: ${car.torque}Nm, Color: ${car.color}, Gears: ${car.gears}
                    System: ${car.fuelSystem}
                    Current Mileage: $latestKm km
                """.trimIndent()

                _state.update { it.copy(
                    isLoading = false,
                    carName = car.displayName
                ) }
            }
            
            // Collect messages from Firestore
            carRepository.getDiagnosisMessages(carId).collect { messages ->
                _state.update { it.copy(
                    messages = if (messages.isEmpty()) {
                        listOf(ChatMessage("Hello! I am your car assistant. How can I help you with your ${car?.make ?: "car"} today?", false))
                    } else {
                        messages
                    }
                ) }
            }
        }
    }

    fun sendMessage(text: String) {
        val carId = currentCarId ?: return
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(text, true)
        
        viewModelScope.launch {
            // Clear previous error
            _state.update { it.copy(errorMessage = null) }
            
            // Save user message to Firestore
            try {
                carRepository.addDiagnosisMessage(carId, userMessage)
            } catch (e: Exception) {
                // If writing to Firestore fails (e.g. App Check), show error locally
                _state.update { it.copy(
                    errorMessage = "Firestore Error: ${e.localizedMessage ?: "Check your connection or App Check status"}"
                ) }
                return@launch
            }
            
            _state.update { it.copy(isTyping = true) }

            try {
                val currentMsgs = _state.value.messages
                val currentLanguage = java.util.Locale.getDefault().displayLanguage
                val response = geminiRepository.getDiagnosisResponse(text, carContext, currentMsgs, currentLanguage)
                
                // Handle Function Calls
                val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()
                val functionCalls = parts.mapNotNull { it.functionCall }
                
                var toolConfirmation = ""

                for (functionCall in functionCalls) {
                    when (functionCall.name) {
                        "update_car_spec" -> {
                            val field = functionCall.args?.get("field")?.jsonPrimitive?.contentOrNull
                            val value = functionCall.args?.get("value")?.jsonPrimitive?.contentOrNull
                            if (field != null && value != null) {
                                handleUpdateCarSpec(field, value)
                                toolConfirmation += "Updated $field to $value. "
                            }
                        }
                        "update_car_mileage" -> {
                            val km = functionCall.args?.get("km")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            if (km != null) {
                                handleUpdateCarMileage(km)
                                toolConfirmation += "Updated mileage to $km km. "
                            }
                        }
                    }
                }

                val aiResponseText = response.text
                if (!aiResponseText.isNullOrBlank()) {
                    val cleanedResponse = cleanAiResponse(aiResponseText)
                    val aiMessage = ChatMessage(cleanedResponse, false)
                    carRepository.addDiagnosisMessage(carId, aiMessage)
                } else if (toolConfirmation.isNotEmpty()) {
                    val aiMessage = ChatMessage(toolConfirmation.trim(), false)
                    carRepository.addDiagnosisMessage(carId, aiMessage)
                }
                
            } catch (t: Throwable) {
                val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
                val error = when {
                    t.message?.contains("403") == true || t.message?.contains("PERMISSION_DENIED") == true -> {
                        "AI Access Denied: Please add this SHA-1 to Google Cloud Console for package ${context.packageName}: \n\n$sha1"
                    }
                    t.message?.contains("404") == true -> {
                        "AI Model Not Found: Check if 'gemini-1.5-flash' is the correct model name in your config."
                    }
                    else -> "AI Error: ${t.localizedMessage ?: "Unknown error"}"
                }
                
                _state.update { it.copy(
                    errorMessage = error
                ) }
            } finally {
                _state.update { it.copy(isTyping = false) }
            }
        }
    }

    private suspend fun handleUpdateCarMileage(km: Double) {
        val carId = currentCarId ?: return
        val log = com.dariusepure.caractivitylog.domain.MileageLog(
            km = km,
            date = java.util.Date()
        )
        carRepository.addMileageLog(carId, log)
        // Refresh context to include new mileage
        loadCarData(carId)
    }

    private suspend fun handleUpdateCarSpec(field: String, value: String) {
        val car = currentCar ?: return
        val updatedCar = when (field.lowercase()) {
            "name" -> car.copy(name = value)
            "platecountry" -> car.copy(plateCountry = value.uppercase())
            "make" -> car.copy(make = value)
            "model" -> car.copy(model = value)
            "vin" -> car.copy(vin = value.uppercase())
            "year" -> car.copy(year = value.toDoubleOrNull()?.roundToInt() ?: car.year)
            "enginesize" -> car.copy(engineSize = value)
            "fueltype" -> car.copy(fuelType = value)
            "fuelsystem" -> car.copy(fuelSystem = value)
            "color" -> car.copy(color = value.uppercase())
            "power" -> car.copy(power = value.toDoubleOrNull()?.roundToInt() ?: car.power)
            "powerunit" -> car.copy(powerUnit = value)
            "torque" -> car.copy(torque = value.toDoubleOrNull()?.roundToInt() ?: car.torque)
            "enginecode" -> car.copy(engineCode = value.uppercase())
            "enginelayout" -> car.copy(engineLayout = value)
            "cylinderlayout" -> car.copy(cylinderLayout = value)
            "length" -> car.copy(length = value.toDoubleOrNull()?.roundToInt() ?: car.length)
            "width" -> car.copy(width = value.toDoubleOrNull()?.roundToInt() ?: car.width)
            "height" -> car.copy(height = value.toDoubleOrNull()?.roundToInt() ?: car.height)
            "wheelbase" -> car.copy(wheelbase = value.toDoubleOrNull()?.roundToInt() ?: car.wheelbase)
            "trackwidth" -> car.copy(trackWidth = value.toDoubleOrNull()?.roundToInt() ?: car.trackWidth)
            "emissionstandard" -> car.copy(emissionStandard = value)
            "aspiration" -> car.copy(aspiration = value)
            "fueltankcapacity" -> car.copy(fuelTankCapacity = value.toDoubleOrNull() ?: car.fuelTankCapacity)
            "batterycapacity" -> car.copy(batteryCapacity = value.toDoubleOrNull() ?: car.batteryCapacity)
            "drivetrain" -> car.copy(drivetrain = value)
            "gearboxtype" -> car.copy(gearboxType = value)
            "gears" -> car.copy(gears = value)
            "frontsuspension" -> car.copy(frontSuspension = value)
            "rearsuspension" -> car.copy(rearSuspension = value)
            "frontbrakes" -> car.copy(frontBrakes = value)
            "rearbrakes" -> car.copy(rearBrakes = value)
            "vehicletype" -> car.copy(vehicleType = value)
            "manufacturingcountry" -> car.copy(manufacturingCountry = value)
            "topspeed" -> car.copy(topSpeed = value.toDoubleOrNull() ?: car.topSpeed)
            "weight" -> car.copy(weight = value.toDoubleOrNull()?.roundToInt() ?: car.weight)
            "numberofseats" -> car.copy(numberOfSeats = value.toDoubleOrNull()?.roundToInt() ?: car.numberOfSeats)
            "numberofcylinders" -> car.copy(numberOfCylinders = value.toDoubleOrNull()?.roundToInt() ?: car.numberOfCylinders)
            "valvespercylinder" -> car.copy(valvesPerCylinder = value.toDoubleOrNull()?.roundToInt() ?: car.valvesPerCylinder)
            "numberofdoors" -> car.copy(numberOfDoors = value.toDoubleOrNull()?.roundToInt() ?: car.numberOfDoors)
            "bootspace" -> car.copy(bootSpace = value.toDoubleOrNull()?.roundToInt() ?: car.bootSpace)
            "tirewidth" -> car.copy(tireWidth = value.toDoubleOrNull()?.roundToInt() ?: car.tireWidth)
            "tireaspectratio" -> car.copy(tireAspectRatio = value.toDoubleOrNull()?.roundToInt() ?: car.tireAspectRatio)
            "tirediameter" -> car.copy(tireDiameter = value.toDoubleOrNull()?.roundToInt() ?: car.tireDiameter)
            else -> car
        }
        
        if (updatedCar != car) {
            carRepository.createCar(updatedCar)
            currentCar = updatedCar
            // Refresh context
            loadCarData(car.id)
        }
    }

    fun resetConversation() {
        val carId = currentCarId ?: return
        viewModelScope.launch {
            carRepository.clearDiagnosisMessages(carId)
        }
    }

    private fun cleanAiResponse(text: String): String {
        return text.replace("*", "").replace("#", "")
    }
}
