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
