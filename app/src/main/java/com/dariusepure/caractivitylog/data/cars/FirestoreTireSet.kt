/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.TireSet
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreTireSet(
    @DocumentId val id: String = "",
    val season: String = "SUMMER",
    val brand: String = "",
    val width: Int = 0,
    val ratio: Int = 0,
    val diameter: Int = 0,
    val dotWeek: Int? = null,
    val dotYear: Int? = null,
    val isActive: Boolean = false
)

fun TireSet.toFirebase() = FirestoreTireSet(
    id = this.id,
    season = this.season.name,
    brand = this.brand,
    width = this.width,
    ratio = this.ratio,
    diameter = this.diameter,
    dotWeek = this.dotWeek,
    dotYear = this.dotYear,
    isActive = this.isActive
)

fun FirestoreTireSet.fromFirebase() = TireSet(
    id = this.id,
    season = try { TireSeason.valueOf(this.season) } catch (e: Exception) { TireSeason.SUMMER },
    brand = this.brand,
    width = this.width,
    ratio = this.ratio,
    diameter = this.diameter,
    dotWeek = this.dotWeek,
    dotYear = this.dotYear,
    isActive = this.isActive
)

