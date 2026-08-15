package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.ai.GeminiRepository
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosisUiState())
    val state = _state.asStateFlow()

    private val _currentCar = MutableStateFlow<Car?>(null)

    fun loadCar(carId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val car = carRepository.getCar(carId)
                if (car != null) {
                    _currentCar.value = car
                    carRepository.getDiagnosisMessages(carId).collect { history ->
                        _state.update { it.copy(
                            isLoading = false, 
                            messages = history, 
                            carName = "${car.make} ${car.model}"
                        ) }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, errorMessage = context.getString(R.string.error_car_not_found)) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: context.getString(R.string.error_generic)) }
            }
        }
    }

    fun onSendMessage(carId: String, message: String) {
        val car = _currentCar.value ?: return
        val currentMessages = _state.value.messages
        
        val newUserMessage = ChatMessage(text = message, isUser = true)
        val updatedMessages = currentMessages + newUserMessage
        
        _state.update { it.copy(messages = updatedMessages, isTyping = true) }

        viewModelScope.launch {
            try {
                carRepository.addDiagnosisMessage(carId, newUserMessage)
                
                // Fetch snapshots of car history for rich context
                val mileageLogs = carRepository.getMileageLogs(carId).firstOrNull() ?: emptyList()
                val maintenanceLogs = carRepository.getMaintenanceLogs(carId).firstOrNull() ?: emptyList()
                val fuelLogs = carRepository.getFuelLogs(carId).firstOrNull() ?: emptyList()
                val tireSets = carRepository.getTireSets(carId).firstOrNull() ?: emptyList()
                val inspections = carRepository.getInspections(carId).firstOrNull() ?: emptyList()
                val insurances = carRepository.getInsurances(carId).firstOrNull() ?: emptyList()
                val vignettes = carRepository.getVignettes(carId).firstOrNull() ?: emptyList()

                val currentMileage = mileageLogs.firstOrNull()?.km ?: 0.0
                val activeTire = tireSets.find { it.isActive }
                val lastMaintenance = maintenanceLogs.firstOrNull()
                val avgConsumption = if (fuelLogs.isNotEmpty()) {
                    // Simple average for context
                    val totalKm = fuelLogs.maxOfOrNull { it.km } ?: 0.0
                    val totalLiters = fuelLogs.sumOf { it.liters }
                    if (totalKm > 0) (totalLiters / totalKm) * 100 else null
                } else null

                val carContext = """
                    Informații complete despre vehicul:
                    - Marcă și Model: ${car.make} ${car.model} (${car.year})
                    - Kilometraj actual: ${currentMileage.toInt()} km
                    - Motorizare: ${car.engineSize} cc, ${car.fuelType}, ${car.power} ${car.powerUnit}
                    - Ultima revizie: ${lastMaintenance?.let { "${it.description} la ${it.km.toInt()} km pe data de ${it.date}" } ?: "Nicio înregistrare"}
                    - Anvelope active: ${activeTire?.let { "${it.brand} ${it.width}/${it.ratio} R${it.diameter} (${it.season})" } ?: "Nespecificat"}
                    - Consum mediu: ${avgConsumption?.let { "%.2f L/100km".format(it) } ?: "Necunoscut"}
                    - Status documente (valabilitate):
                        * ITP: ${inspections.firstOrNull()?.expiryDate ?: "Necunoscut"}
                        * RCA: ${insurances.firstOrNull()?.expiryDate ?: "Necunoscut"}
                        * Rovinietă: ${vignettes.firstOrNull()?.expiryDate ?: "Necunoscut"}
                    - Detalii tehnice: Transmisie ${car.gearboxType}, Tracțiune ${car.drivetrain}, Serie șasiu (final): ${car.vin.takeLast(6)}
                """.trimIndent()
                
                val language = if (context.resources.configuration.locales[0].language == "ro") "Romanian" else "English"
                
                val response = geminiRepository.getDiagnosisResponse(
                    prompt = message,
                    carContext = carContext,
                    history = updatedMessages,
                    language = language
                )

                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val aiMessage = ChatMessage(text = aiText, isUser = false)
                
                carRepository.addDiagnosisMessage(carId, aiMessage)
                
                _state.update { it.copy(messages = updatedMessages + aiMessage, isTyping = false) }
            } catch (t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("403") == true || t.message?.contains("PERMISSION_DENIED") == true -> {
                        val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
                        context.getString(R.string.error_ai_access_denied, context.packageName, sha1)
                    }
                    t.message?.contains("404") == true -> {
                        context.getString(R.string.error_ai_model_not_found)
                    }
                    else -> context.getString(R.string.error_ai_generic, t.localizedMessage ?: context.getString(R.string.common_not_applicable))
                }
                _state.update { it.copy(isTyping = false, errorMessage = errorMessage) }
            }
        }
    }

    fun resetChat(carId: String) {
        viewModelScope.launch {
            try {
                carRepository.clearDiagnosisMessages(carId)
                _state.update { it.copy(messages = emptyList()) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.localizedMessage ?: context.getString(R.string.error_generic)) }
            }
        }
    }
}
