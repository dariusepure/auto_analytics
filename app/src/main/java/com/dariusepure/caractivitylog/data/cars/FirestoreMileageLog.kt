package com.dariusepure.caractivitylog.data.cars

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.dariusepure.caractivitylog.domain.MileageLog

data class FirestoreMileageLog(
    @DocumentId val id: String = "",
    val km: Double = 0.0,
    val date: Timestamp = Timestamp.now()
)

fun MileageLog.toFirebase() = FirestoreMileageLog(
    id = this.id,
    km = this.km,
    date = Timestamp(this.date)
)

fun FirestoreMileageLog.fromFirebase() = MileageLog(
    id = this.id,
    km = this.km,
    date = this.date.toDate()
)

