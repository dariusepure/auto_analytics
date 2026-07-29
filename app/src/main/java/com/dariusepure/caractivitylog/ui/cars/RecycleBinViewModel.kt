package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.util.LocalImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
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
                LocalImageHelper.deleteCarImage(context, carId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
