/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.AiAnalysis
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class FirestoreAiAnalysis(
    @DocumentId val id: String = "",
    val summary: String = "",
    val healthScore: Int = 100,
    val recommendations: List<String> = emptyList(),
    val analyzedAt: Timestamp = Timestamp.now()
)

fun AiAnalysis.toFirebase() = FirestoreAiAnalysis(
    summary = this.summary,
    healthScore = this.healthScore,
    recommendations = this.recommendations,
    analyzedAt = Timestamp(this.analyzedAt)
)

fun FirestoreAiAnalysis.fromFirebase(carId: String) = AiAnalysis(
    summary = this.summary,
    healthScore = this.healthScore,
    recommendations = this.recommendations,
    analyzedAt = this.analyzedAt.toDate(),
    carId = carId
)

