package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Insurance
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class InsuranceHistoryUiState {
    object Loading : InsuranceHistoryUiState()
    data class Success(
        val insurances: List<Insurance>,
        val stats: InsuranceStats
    ) : InsuranceHistoryUiState()
    data class Error(val message: String) : InsuranceHistoryUiState()
}

data class InsuranceStats(
    val latestExpiryDate: Date?,
    val daysRemaining: Long?
)

@HiltViewModel
class InsuranceHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<InsuranceHistoryUiState>(InsuranceHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = InsuranceHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val insurancesFlow = carRepository.getInsurances(carId)

                combine(carFlow, insurancesFlow) { car, insurances ->
                    if (car != null) {
                        val sortedInsurances = insurances.sortedByDescending { it.date }
                        val latest = sortedInsurances.firstOrNull()
                        val days = latest?.let {
                            val diff = it.expiryDate.time - Date().time
                            diff / (1000 * 60 * 60 * 24)
                        }
                        val stats = InsuranceStats(latest?.expiryDate, days)
                        InsuranceHistoryUiState.Success(sortedInsurances, stats)
                    } else {
                        InsuranceHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = InsuranceHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    fun addInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            carRepository.addInsurance(carId, insurance)
        }
    }

    fun updateInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            carRepository.updateInsurance(carId, insurance)
        }
    }

    fun deleteInsurance(carId: String, insuranceId: String) {
        viewModelScope.launch {
            carRepository.deleteInsurance(carId, insuranceId)
        }
    }
}
