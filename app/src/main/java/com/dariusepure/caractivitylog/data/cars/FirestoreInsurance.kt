package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Insurance
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class FirestoreInsurance(
    @DocumentId val id: String = "",
    val date: Timestamp = Timestamp.now(),
    val durationValue: Int = 1,
    val durationUnit: String = "MONTHS",
    val provider: String = "",
    val cost: Double = 0.0
)

fun Insurance.toFirebase() = FirestoreInsurance(
    id = this.id,
    date = Timestamp(this.date),
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    provider = this.provider,
    cost = this.cost
)

fun FirestoreInsurance.fromFirebase() = Insurance(
    id = this.id,
    date = this.date.toDate(),
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.MONTHS },
    provider = this.provider,
    cost = this.cost
)
