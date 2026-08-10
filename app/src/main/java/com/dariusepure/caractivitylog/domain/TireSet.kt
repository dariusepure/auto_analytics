/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

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

