package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Vignette
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class FirestoreVignette(
    @DocumentId val id: String = "",
    val date: Timestamp = Timestamp.now(),
    val durationValue: Int = 1,
    val durationUnit: String = "MONTHS",
    val country: String = ""
)

fun Vignette.toFirebase() = FirestoreVignette(
    id = this.id,
    date = Timestamp(this.date),
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    country = this.country
)

fun FirestoreVignette.fromFirebase() = Vignette(
    id = this.id,
    date = this.date.toDate(),
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.MONTHS },
    country = this.country
)
