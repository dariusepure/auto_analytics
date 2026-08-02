package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecycleBinUiState {
    data object Loading : RecycleBinUiState
    data class Success(val cars: List<Car>) : RecycleBinUiState
    data class Error(val message: String) : RecycleBinUiState
    data object Empty : RecycleBinUiState
}

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val carRepository: CarRepository
) : ViewModel() {

    val state: StateFlow<RecycleBinUiState> = carRepository.deletedCars
        .map { cars ->
            if (cars.isEmpty()) RecycleBinUiState.Empty
            else RecycleBinUiState.Success(cars)
        }
        .catch { e ->
            emit(RecycleBinUiState.Error(e.localizedMessage ?: "An error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecycleBinUiState.Loading
        )

    fun onRestoreCar(carId: String) {
        viewModelScope.launch {
            try {
                carRepository.restoreCar(carId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onPermanentlyDeleteCar(carId: String) {
        viewModelScope.launch {
            try {
                carRepository.permanentlyDeleteCar(carId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
