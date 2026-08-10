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

data class CarReport(
    val id: String = "",
    val carId: String = "",
    val fileName: String = "", // Filename in internal storage
    val date: Date = Date()
)

