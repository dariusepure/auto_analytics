package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.Vignette
import com.dariusepure.caractivitylog.domain.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class VignetteStats(
    val latestExpiryDate: Date? = null,
    val daysRemaining: Long? = null
)

sealed class VignetteHistoryUiState {
    object Loading : VignetteHistoryUiState()
    data class Success(
        val car: Car,
        val vignettes: List<Vignette>,
        val stats: VignetteStats,
        val unitSystem: UnitSystem
    ) : VignetteHistoryUiState()
    data class Error(val message: String) : VignetteHistoryUiState()
}

@HiltViewModel
class VignetteHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<VignetteHistoryUiState>(VignetteHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getVignettes(carId),
                preferenceRepository.unitSystem
            ) { car, vignettes, unitSystem ->
                if (car != null) {
                    val latest = vignettes.maxByOrNull { it.date }
                    val stats = VignetteStats(
                        latestExpiryDate = latest?.expiryDate,
                        daysRemaining = latest?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) }
                    )
                    VignetteHistoryUiState.Success(car, vignettes.sortedByDescending { it.date }, stats, unitSystem)
                } else {
                    VignetteHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    fun addVignette(carId: String, vignette: Vignette) {
        viewModelScope.launch {
            carRepository.addVignette(carId, vignette)
        }
    }

    fun updateVignette(carId: String, vignette: Vignette) {
        viewModelScope.launch {
            carRepository.updateVignette(carId, vignette)
        }
    }

    fun deleteVignette(carId: String, vignetteId: String) {
        viewModelScope.launch {
            carRepository.deleteVignette(carId, vignetteId)
        }
    }
}
