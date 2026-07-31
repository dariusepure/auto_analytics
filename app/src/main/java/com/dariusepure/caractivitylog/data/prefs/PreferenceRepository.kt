package com.dariusepure.caractivitylog.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Currency(
    val name: String,
    val code: String,
    val symbol: String
)

val supportedCurrencies = listOf(
    Currency("Romanian Leu", "RON", "RON"),
    Currency("Euro", "EUR", "€"),
    Currency("US Dollar", "USD", "$"),
    Currency("British Pound", "GBP", "£")
)

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_mode")) prefs.getBoolean("is_dark_mode", false) else null
    )
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _currencyCode = MutableStateFlow(
        prefs.getString("currency_code", "RON") ?: "RON"
    )
    val currencyCode = _currencyCode.asStateFlow()

    fun setDarkMode(enabled: Boolean?) {
        _isDarkMode.value = enabled
        if (enabled == null) {
            prefs.edit().remove("is_dark_mode").apply()
        } else {
            prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        }
    }

    fun setCurrency(code: String) {
        _currencyCode.value = code
        prefs.edit().putString("currency_code", code).apply()
    }
}
