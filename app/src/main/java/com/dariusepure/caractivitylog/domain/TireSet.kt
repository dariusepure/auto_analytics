package com.dariusepure.caractivitylog.domain

import androidx.annotation.StringRes
import com.dariusepure.caractivitylog.R

data class TireSet(
    val id: String = "",
    val season: TireSeason = TireSeason.SUMMER,
    val brand: String = "",
    val model: String = "",
    val width: Int = 0,
    val ratio: Int = 0,
    val diameter: Int = 0,
    val dotWeek: Int? = null,
    val dotYear: Int? = null,
    val isActive: Boolean = false
)

enum class TireSeason(@StringRes val labelRes: Int) {
    SUMMER(R.string.tire_season_summer),
    WINTER(R.string.tire_season_winter),
    ALL_SEASON(R.string.tire_season_all_season)
}
