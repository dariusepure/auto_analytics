/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.domain

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class AiAnalysis(
    val summary: String = "",
    val healthScore: Int = 100, // 0 to 100
    val recommendations: List<String> = emptyList(),
    @kotlinx.serialization.Transient val analyzedAt: Date = Date(),
    @kotlinx.serialization.Transient val carId: String = ""
)

