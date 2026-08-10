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

