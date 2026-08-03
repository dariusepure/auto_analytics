package com.dariusepure.caractivitylog.ui.theme

import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : androidx.lifecycle.ViewModel() {
    
    val isDarkMode = preferenceRepository.isDarkMode

    fun toggleTheme(currentDark: Boolean) {
        preferenceRepository.setDarkMode(!currentDark)
    }

    fun setDarkMode(enabled: Boolean?) {
        preferenceRepository.setDarkMode(enabled)
    }
}
