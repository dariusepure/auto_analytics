package com.dariusepure.caractivitylog.ui.theme

import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.UnitSystem
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : androidx.lifecycle.ViewModel() {
    
    val isDarkMode = preferenceRepository.isDarkMode
    val unitSystem = preferenceRepository.unitSystem

    fun toggleTheme(currentDark: Boolean) {
        preferenceRepository.setDarkMode(!currentDark)
    }

    fun setDarkMode(enabled: Boolean?) {
        preferenceRepository.setDarkMode(enabled)
    }

    fun setUnitSystem(system: UnitSystem) {
        preferenceRepository.setUnitSystem(system)
    }
}
