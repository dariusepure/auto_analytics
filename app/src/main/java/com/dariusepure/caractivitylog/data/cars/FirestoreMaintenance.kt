package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.Maintenance
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class FirestoreMaintenance(
    @DocumentId val id: String = "",
    val date: Date = Date(),
    val km: Double = 0.0,
    val description: String = "",
    val mileageLogId: String = "",
    val category: String = "General"
)

fun Maintenance.toFirebase() = FirestoreMaintenance(
    id = this.id,
    date = this.date,
    km = this.km,
    description = this.description,
    mileageLogId = this.mileageLogId,
    category = this.category
)

fun FirestoreMaintenance.fromFirebase() = Maintenance(
    id = this.id,
    date = this.date,
    km = this.km,
    description = this.description,
    mileageLogId = this.mileageLogId,
    category = this.category
)
