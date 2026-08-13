package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.TireSet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TireHistoryUiState {
    object Loading : TireHistoryUiState()
    data class Success(
        val tireSets: List<TireSet>,
        val stats: TireStats
    ) : TireHistoryUiState()
    data class Error(val message: String) : TireHistoryUiState()
}

data class TireStats(
    val totalSets: Int,
    val activeTireSet: TireSet?
)

@HiltViewModel
class TireHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<TireHistoryUiState>(TireHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = TireHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val tireSetsFlow = carRepository.getTireSets(carId)

                combine(carFlow, tireSetsFlow) { car, tireSets ->
                    if (car != null) {
                        val active = tireSets.find { it.isActive }
                        val stats = TireStats(tireSets.size, active)
                        TireHistoryUiState.Success(tireSets, stats)
                    } else {
                        TireHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = TireHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
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
