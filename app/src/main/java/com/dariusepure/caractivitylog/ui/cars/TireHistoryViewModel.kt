package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TireStats(
    val activeTireSet: TireSet? = null,
    val totalSets: Int = 0
)

sealed class TireHistoryUiState {
    object Loading : TireHistoryUiState()
    data class Success(
        val car: Car,
        val tireSets: List<TireSet>,
        val stats: TireStats,
        val unitSystem: UnitSystem
    ) : TireHistoryUiState()
    data class Error(val message: String) : TireHistoryUiState()
}

@HiltViewModel
class TireHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TireHistoryUiState>(TireHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getTireSets(carId),
                preferenceRepository.unitSystem
            ) { car, tireSets, unitSystem ->
                if (car != null) {
                    val stats = TireStats(
                        activeTireSet = tireSets.find { it.isActive },
                        totalSets = tireSets.size
                    )
                    TireHistoryUiState.Success(car, tireSets, stats, unitSystem)
                } else {
                    TireHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    fun addTireSet(carId: String, tireSet: TireSet) {
        viewModelScope.launch {
            carRepository.addTireSet(carId, tireSet)
        }
    }

    fun updateTireSet(carId: String, tireSet: TireSet) {
        viewModelScope.launch {
            carRepository.updateTireSet(carId, tireSet)
        }
    }

    fun deleteTireSet(carId: String, tireSetId: String) {
        viewModelScope.launch {
            carRepository.deleteTireSet(carId, tireSetId)
        }
    }
}
