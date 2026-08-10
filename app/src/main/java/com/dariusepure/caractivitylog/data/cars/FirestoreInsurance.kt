/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

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
    val provider: String = ""
)

fun Insurance.toFirebase() = FirestoreInsurance(
    id = this.id,
    date = Timestamp(this.date),
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    provider = this.provider
)

fun FirestoreInsurance.fromFirebase() = Insurance(
    id = this.id,
    date = this.date.toDate(),
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.MONTHS },
    provider = this.provider
)

