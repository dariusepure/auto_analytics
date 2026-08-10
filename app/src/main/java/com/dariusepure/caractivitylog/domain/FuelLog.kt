/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.domain

import java.util.Date

data class FuelLog(
    val id: String = "",
    val date: Date = Date(),
    val km: Double = 0.0,
    val liters: Double = 0.0,
    val isFullTank: Boolean = true,
    val mileageLogId: String = ""
)

