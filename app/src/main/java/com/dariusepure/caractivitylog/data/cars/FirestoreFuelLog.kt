package com.dariusepure.caractivitylog.data.cars

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.dariusepure.caractivitylog.domain.FuelLog

data class FirestoreFuelLog(
    @DocumentId val id: String = "",
    val date: Timestamp = Timestamp.now(),
    val km: Double = 0.0,
    val liters: Double = 0.0,
    val isFullTank: Boolean = true,
    val mileageLogId: String = ""
)

fun FuelLog.toFirebase() = FirestoreFuelLog(
    id = this.id,
    date = Timestamp(this.date),
    km = this.km,
    liters = this.liters,
    isFullTank = this.isFullTank,
    mileageLogId = this.mileageLogId
)

fun FirestoreFuelLog.fromFirebase() = FuelLog(
    id = this.id,
    date = this.date.toDate(),
    km = this.km,
    liters = this.liters,
    isFullTank = this.isFullTank,
    mileageLogId = this.mileageLogId
)
