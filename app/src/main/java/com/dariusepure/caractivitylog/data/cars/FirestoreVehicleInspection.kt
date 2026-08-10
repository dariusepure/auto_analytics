/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.cars

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.VehicleInspection

data class FirestoreVehicleInspection(
    @DocumentId val id: String = "",
    val date: Timestamp = Timestamp.now(),
    val mileage: Double = 0.0,
    val durationValue: Int = 1,
    val durationUnit: String = "YEARS",
    val mileageLogId: String = ""
)

fun VehicleInspection.toFirebase() = FirestoreVehicleInspection(
    id = this.id,
    date = Timestamp(this.date),
    mileage = this.mileage,
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    mileageLogId = this.mileageLogId
)

fun FirestoreVehicleInspection.fromFirebase() = VehicleInspection(
    id = this.id,
    date = this.date.toDate(),
    mileage = this.mileage,
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.YEARS },
    mileageLogId = this.mileageLogId
)

