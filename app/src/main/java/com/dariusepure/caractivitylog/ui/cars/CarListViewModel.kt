package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import androidx.annotation.StringRes
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.displayName
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

enum class CarSortOrder(@StringRes val labelRes: Int) {
    DATE_ADDED(R.string.sort_default),
    BRAND(R.string.sort_brand),
    BRAND_DESC(R.string.sort_brand_desc),
    YEAR(R.string.sort_year),
    YEAR_ASC(R.string.sort_year_oldest),
    POWER_DESC(R.string.sort_power),
    NAME(R.string.sort_name),
    FUEL_TYPE(R.string.sort_fuel_type)
}

@HiltViewModel
class CarListViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(CarSortOrder.DATE_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val state: StateFlow<CarListUiState> = combine(
        carRepository.cars,
        _sortOrder,
        _searchQuery
    ) { cars, order, query ->
        if (cars.isEmpty()) {
            CarListUiState.Empty
        } else {
            val filteredCars = if (query.isBlank()) {
                cars
            } else {
                cars.filter { car ->
                    car.make.contains(query, ignoreCase = true) ||
                            car.model.contains(query, ignoreCase = true) ||
                            car.displayName.contains(query, ignoreCase = true) ||
                            car.licensePlate.contains(query, ignoreCase = true) ||
                            car.vin.contains(query, ignoreCase = true)
                }
            }

            val sortedCars = when (order) {
                CarSortOrder.DATE_ADDED -> filteredCars
                CarSortOrder.BRAND -> filteredCars.sortedBy { it.make }
                CarSortOrder.BRAND_DESC -> filteredCars.sortedByDescending { it.make }
                CarSortOrder.YEAR -> filteredCars.sortedByDescending { it.year }
                CarSortOrder.YEAR_ASC -> filteredCars.sortedBy { it.year }
                CarSortOrder.POWER_DESC -> filteredCars.sortedByDescending { it.power }
                CarSortOrder.NAME -> filteredCars.sortedBy { it.displayName }
                CarSortOrder.FUEL_TYPE -> filteredCars.sortedBy { it.fuelType }
            }
            CarListUiState.Success(sortedCars)
        }
    }
        .catch { e ->
            emit(CarListUiState.Error(e.localizedMessage ?: "An error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = run {
                val cached = carRepository.getCarsFromCache()
                if (cached.isNotEmpty()) {
                    CarListUiState.Success(cached)
                } else {
                    CarListUiState.Loading
                }
            }
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                carRepository.refresh()
            } catch (e: Exception) {
                // Log error
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onSortOrderChanged(order: CarSortOrder) {
        _sortOrder.value = order
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}

