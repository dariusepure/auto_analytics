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
import com.dariusepure.caractivitylog.domain.ScannedMileageEntry
import com.dariusepure.caractivitylog.domain.VehicleInspection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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
        val fuelLogs: List<FuelLog> = emptyList(),
        val isScanning: Boolean = false
    ) : CarDetailsUiState()
    data class Error(val message: String) : CarDetailsUiState()
}

@HiltViewModel
class CarDetailsViewModel @Inject constructor(
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
                combine(
                    carRepository.getCarFlow(carId),
                    carRepository.getMileageLogs(carId),
                    carRepository.getInspections(carId),
                    carRepository.getFuelLogs(carId)
                ) { car, logs, inspections, fuelLogs ->
                    if (car != null) {
                        val currentScanning = (_state.value as? CarDetailsUiState.Success)?.isScanning ?: false
                        CarDetailsUiState.Success(car, logs, inspections, fuelLogs, currentScanning)
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
                .onSuccess { data ->
                    _state.value = currentState.copy(isScanning = false)
                    val entries = mutableListOf<ScannedMileageEntry>()
                    data.mileage?.let { entries.add(ScannedMileageEntry(it)) }
                    data.mileageHistory?.let { entries.addAll(it) }
                    
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
                .onSuccess { data ->
                    _state.value = currentState.copy(isScanning = false)
                    val entries = mutableListOf<ScannedMileageEntry>()
                    data.mileage?.let { entries.add(ScannedMileageEntry(it)) }
                    data.mileageHistory?.let { entries.addAll(it) }

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
                    carRepository.addMileageLog(carId, MileageLog(km = entry.km, date = date))
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

    fun deleteInspection(carId: String, inspectionId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteInspection(carId, inspectionId)
            } catch (e: Exception) {
                _uiEvent.trySend(CarDetailsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
            }
        }
    }

}
