package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.cars.CarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CarSortOrder(val label: String) {
    DATE_ADDED("Default"),
    BRAND("Brand (A-Z)"),
    YEAR("Year (Newest)")
}

@HiltViewModel
class CarListViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(CarSortOrder.DATE_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    val state: StateFlow<CarListUiState> = combine(
        carRepository.cars,
        _sortOrder
    ) { cars, order ->
        if (cars.isEmpty()) {
            CarListUiState.Empty
        } else {
            val sortedCars = when (order) {
                CarSortOrder.DATE_ADDED -> cars // Default repo order (desc by update)
                CarSortOrder.BRAND -> cars.sortedBy { it.make }
                CarSortOrder.YEAR -> cars.sortedByDescending { it.year }
            }
            CarListUiState.Success(sortedCars)
        }
    }
        .catch { e ->
            emit(CarListUiState.Error(e.localizedMessage ?: "An error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarListUiState.Loading
        )

    fun onSortOrderChanged(order: CarSortOrder) {
        _sortOrder.value = order
    }

    fun onDeleteCar(carId: String) {
        viewModelScope.launch {
            try {
                carRepository.deleteCar(carId)
            } catch (e: Exception) {
                // Log or handle error if needed
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                authRepository.setGuestMode(false)
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
