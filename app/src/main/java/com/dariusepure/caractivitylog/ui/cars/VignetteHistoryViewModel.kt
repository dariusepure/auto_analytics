package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Vignette
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class VignetteHistoryUiState {
    object Loading : VignetteHistoryUiState()
    data class Success(
        val vignettes: List<Vignette>,
        val stats: VignetteStats
    ) : VignetteHistoryUiState()
    data class Error(val message: String) : VignetteHistoryUiState()
}

data class VignetteStats(
    val latestExpiryDate: Date?,
    val daysRemaining: Long?
)

@HiltViewModel
class VignetteHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<VignetteHistoryUiState>(VignetteHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = VignetteHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val vignettesFlow = carRepository.getVignettes(carId)

                combine(carFlow, vignettesFlow) { car, vignettes ->
                    if (car != null) {
                        val sortedVignettes = vignettes.sortedByDescending { it.date }
                        val latest = sortedVignettes.firstOrNull()
                        val days = latest?.let {
                            val diff = it.expiryDate.time - Date().time
                            diff / (1000 * 60 * 60 * 24)
                        }
                        val stats = VignetteStats(latest?.expiryDate, days)
                        VignetteHistoryUiState.Success(sortedVignettes, stats)
                    } else {
                        VignetteHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = VignetteHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
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
