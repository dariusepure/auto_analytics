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
