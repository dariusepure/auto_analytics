package com.dariusepure.caractivitylog.ui.cars

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.ai.GeminiRepository
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.Vignette
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.ScannedMileageEntry
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.AiAnalysis
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class CarDetailsUiEvent {
    data class ShowToast(val message: String) : CarDetailsUiEvent()
}

sealed class CarDetailsUiState {
    object Loading : CarDetailsUiState()
    data class Success(
        val car: Car,
        val mileageLogs: List<MileageLog>,
        val inspections: List<VehicleInspection>,
        val insurances: List<Insurance> = emptyList(),
        val vignettes: List<Vignette> = emptyList(),
        val tireSets: List<TireSet> = emptyList(),
        val fuelLogs: List<FuelLog> = emptyList(),
        val maintenanceLogs: List<Maintenance> = emptyList(),
        val aiAnalysis: AiAnalysis? = null,
        val isScanning: Boolean = false,
        val isAnalyzing: Boolean = false
    ) : CarDetailsUiState()
    data class Error(val message: String) : CarDetailsUiState()
}

@HiltViewModel
class CarDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CarDetailsUiState>(CarDetailsUiState.Loading)
    val state = _state.asStateFlow()
    
    private val _uiEvent = Channel<CarDetailsUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _scannedMileageEvent = Channel<List<ScannedMileageEntry>>(Channel.BUFFERED)
    val scannedMileageEvent = _scannedMileageEvent.receiveAsFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    fun loadCarData(carId: String) {
        viewModelScope.launch {
            _state.value = CarDetailsUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val mileageFlow = carRepository.getMileageLogs(carId)
                val inspectionsFlow = carRepository.getInspections(carId)
                val insurancesFlow = carRepository.getInsurances(carId)
                val vignettesFlow = carRepository.getVignettes(carId)
                val tireSetsFlow = carRepository.getTireSets(carId)
                val fuelLogsFlow = carRepository.getFuelLogs(carId)
                val maintenanceFlow = carRepository.getMaintenanceLogs(carId)
                val aiAnalysisFlow = carRepository.getAiAnalysis(carId)

                @Suppress("UNCHECKED_CAST")
                kotlinx.coroutines.flow.combine(
                    carFlow,
                    mileageFlow,
                    inspectionsFlow,
                    insurancesFlow,
                    vignettesFlow,
                    tireSetsFlow,
                    fuelLogsFlow,
                    maintenanceFlow,
                    aiAnalysisFlow
                ) { args: Array<Any?> ->
                    val car = args[0] as? Car
                    val logs = args[1] as List<MileageLog>
                    val inspections = args[2] as List<VehicleInspection>
                    val insurances = args[3] as List<Insurance>
                    val vignettes = args[4] as List<Vignette>
                    val tireSets = args[5] as List<TireSet>
                    val fuelLogs = args[6] as List<FuelLog>
                    val maintenance = args[7] as List<Maintenance>
                    val aiAnalysis = args[8] as? AiAnalysis

                    if (car != null) {
                        val currentState = _state.value as? CarDetailsUiState.Success
                        val currentScanning = currentState?.isScanning ?: false
                        val currentAnalyzing = currentState?.isAnalyzing ?: false
                        CarDetailsUiState.Success(
                            car, logs, inspections, insurances, vignettes, 
                            tireSets, fuelLogs, maintenance, aiAnalysis, 
                            currentScanning, currentAnalyzing
                        )
                    } else {
                        CarDetailsUiState.Error("Car not found")
                    }
                }.collect { newState ->
                    _state.value = newState
                }
            } catch (e: Exception) {
                _state.value = CarDetailsUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun scanImage(bitmap: Bitmap) {
        val currentState = _state.value as? CarDetailsUiState.Success ?: return
        _state.value = currentState.copy(isScanning = true)
        
        viewModelScope.launch {
            geminiRepository.scanRegistrationCertificate(bitmap)
                .onSuccess { dataList ->
                    _state.value = currentState.copy(isScanning = false)
                    val entries = mutableListOf<ScannedMileageEntry>()
                    dataList.forEach { data ->
                        data.mileage?.let { entries.add(ScannedMileageEntry(it)) }
                        data.mileageHistory?.let { entries.addAll(it) }
                    }
                    
                    if (entries.isNotEmpty()) {
                        _scannedMileageEvent.trySend(entries.distinctBy { it.km })
                    } else {
                        _uiEvent.trySend(CarDetailsUiEvent.ShowToast("No mileage found in photo"))
                    }
                }
                .onFailure { e ->
                    _state.value = currentState.copy(isScanning = false)
                    _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Scan failed: ${e.localizedMessage}"))
                }
        }
    }

    fun scanDocument(uri: Uri, mimeType: String) {
        val currentState = _state.value as? CarDetailsUiState.Success ?: return
        _state.value = currentState.copy(isScanning = true)
        
        viewModelScope.launch {
            geminiRepository.scanDocument(uri, mimeType)
                .onSuccess { dataList ->
                    _state.value = currentState.copy(isScanning = false)
                    val entries = mutableListOf<ScannedMileageEntry>()
                    dataList.forEach { data ->
                        data.mileage?.let { entries.add(ScannedMileageEntry(it)) }
                        data.mileageHistory?.let { entries.addAll(it) }
                    }

                    if (entries.isNotEmpty()) {
                        _scannedMileageEvent.trySend(entries.distinctBy { it.km })
                    } else {
                        _uiEvent.trySend(CarDetailsUiEvent.ShowToast("No mileage records found in document"))
                    }
                }
                .onFailure { e ->
                    _state.value = currentState.copy(isScanning = false)
                    _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Scan failed: ${e.localizedMessage}"))
                }
        }
    }

    fun addMileage(carId: String, km: Double, date: Date) {
        viewModelScope.launch {
            try {
                carRepository.addMileageLog(carId, MileageLog(km = km, date = date))
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun addBatchMileage(carId: String, entries: List<ScannedMileageEntry>) {
        viewModelScope.launch {
            try {
                entries.forEach { entry ->
                    val date = try {
                        entry.date?.let { dateFormat.parse(it) } ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    val km = entry.km ?: return@forEach
                    carRepository.addMileageLog(carId, MileageLog(km = km, date = date))
                }
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Successfully added ${entries.size} records"))
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateMileage(carId: String, log: MileageLog) {
        viewModelScope.launch {
            try {
                carRepository.updateMileageLog(carId, log)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteMileage(carId: String, logId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteMileageLog(carId, logId)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }
    
    // Unused but kept for existing API compatibility
    fun addInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            try {
                carRepository.addInspection(carId, inspection)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            try {
                carRepository.updateInspection(carId, inspection)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            try {
                carRepository.deleteInspection(carId, inspection)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun addInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            try {
                carRepository.addInsurance(carId, insurance)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            try {
                carRepository.updateInsurance(carId, insurance)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteInsurance(carId: String, insuranceId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteInsurance(carId, insuranceId)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun addVignette(carId: String, vignette: Vignette) {
        viewModelScope.launch {
            try {
                carRepository.addVignette(carId, vignette)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateVignette(carId: String, vignette: Vignette) {
        viewModelScope.launch {
            try {
                carRepository.updateVignette(carId, vignette)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteVignette(carId: String, vignetteId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteVignette(carId, vignetteId)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun addTireSet(carId: String, tireSet: TireSet) {
        viewModelScope.launch {
            try {
                carRepository.addTireSet(carId, tireSet)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateTireSet(carId: String, tireSet: TireSet) {
        viewModelScope.launch {
            try {
                carRepository.updateTireSet(carId, tireSet)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteTireSet(carId: String, tireSetId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteTireSet(carId, tireSetId)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun addMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            try {
                carRepository.addMaintenanceLog(carId, log)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun updateMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            try {
                carRepository.updateMaintenanceLog(carId, log)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            try {
                carRepository.deleteMaintenanceLog(carId, log)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun fetchModelInsights(carId: String) {
        val currentState = _state.value as? CarDetailsUiState.Success ?: return
        _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = true) else it }

        viewModelScope.launch {
            val car = currentState.car
            val language = java.util.Locale.getDefault().displayLanguage
            geminiRepository.fetchModelInsights(car.make, car.model, car.year, language)
                .onSuccess { insights ->
                    carRepository.createCar(car.copy(modelWikiSummary = insights))
                    _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = false) else it }
                }
                .onFailure { e ->
                    _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = false) else it }
                    _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Failed to fetch insights: ${e.localizedMessage}"))
                }
        }
    }

    fun analyzeCarHealth(carId: String) {
        val currentState = _state.value as? CarDetailsUiState.Success ?: return
        _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = true) else it }
        
        viewModelScope.launch {
            try {
                val car = currentState.car
                val fuelLogs = currentState.fuelLogs.take(5).joinToString("\n") { 
                    "${it.date}: ${it.km} km, ${it.liters} L" 
                }
                val maintenanceHistory = currentState.maintenanceLogs.take(10).joinToString("\n") {
                    "${it.date}: ${it.description} at ${it.km} km"
                }
                
                val carContext = """
                    Make: ${car.make}, Model: ${car.model}, Year: ${car.year}
                    Engine: ${car.engineSize} ${car.fuelType}
                    Power: ${car.power}${car.powerUnit}
                """.trimIndent()

                val currentLanguage = java.util.Locale.getDefault().displayLanguage
                geminiRepository.generateHealthAnalysis(carContext, fuelLogs, maintenanceHistory, currentLanguage)
                    .onSuccess { analysis ->
                        carRepository.saveAiAnalysis(carId, analysis)
                        _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = false) else it }
                    }
                    .onFailure { e ->
                        val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
                        val message = if (e.message?.contains("403") == true) {
                            "Analysis Access Denied. Verify SHA-1 in Google Cloud: $sha1"
                        } else {
                            "Analysis failed: ${e.localizedMessage}"
                        }
                        _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = false) else it }
                        _uiEvent.trySend(CarDetailsUiEvent.ShowToast(message))
                    }
            } catch (e: Exception) {
                _state.update { if (it is CarDetailsUiState.Success) it.copy(isAnalyzing = false) else it }
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }
}
