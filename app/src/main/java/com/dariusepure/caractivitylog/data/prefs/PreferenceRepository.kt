package com.dariusepure.caractivitylog.data.prefs

import android.content.Context
import com.dariusepure.caractivitylog.domain.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_mode")) prefs.getBoolean("is_dark_mode", false) else null
    )
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _unitSystem = MutableStateFlow(
        UnitSystem.valueOf(prefs.getString("unit_system", UnitSystem.METRIC.name) ?: UnitSystem.METRIC.name)
    )
    val unitSystem = _unitSystem.asStateFlow()

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode = _isGuestMode.asStateFlow()

    fun setDarkMode(enabled: Boolean?) {
        _isDarkMode.value = enabled
        if (enabled == null) {
            prefs.edit().remove("is_dark_mode").apply()
        } else {
            prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        }
    }

    fun setUnitSystem(system: UnitSystem) {
        _unitSystem.value = system
        prefs.edit().putString("unit_system", system.name).apply()
    }

    fun setGuestMode(enabled: Boolean) {
        _isGuestMode.value = enabled
        prefs.edit().putBoolean("is_guest_mode", enabled).apply()
    }
}

