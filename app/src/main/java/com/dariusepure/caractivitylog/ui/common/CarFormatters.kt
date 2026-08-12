/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.common

import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.VehicleInspection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object CarFormatters {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private const val MILE_RATIO = 1.609344

    fun toCanonicalDistance(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value * MILE_RATIO else value
    }

    fun fromCanonicalDistance(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value / MILE_RATIO else value
    }

    fun toCanonicalSpeed(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value * MILE_RATIO else value
    }

    fun fromCanonicalSpeed(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value / MILE_RATIO else value
    }

    fun formatPower(context: android.content.Context, car: Car): String {
        val hpValue: Int
        val kwValue: Int
        
        if (car.powerUnit.lowercase() == "kw") {
            kwValue = car.power
            hpValue = (car.power * 1.35962).roundToInt()
        } else {
            hpValue = car.power
            kwValue = (car.power / 1.35962).roundToInt()
        }
        
        return context.getString(com.dariusepure.caractivitylog.R.string.formatter_power_dual, hpValue, kwValue)
    }

    fun getCarSummary(context: android.content.Context, car: Car): String {
        val details = mutableListOf<String>()
        if (car.year != 0) details.add(car.year.toString())
        if (car.fuelType.isNotBlank()) details.add(CarTranslations.getFuelTypeLabel(context, car.fuelType))
        if (car.power != 0) {
            val hp = if (car.powerUnit.lowercase() == "hp") car.power else (car.power * 1.35962).roundToInt()
            details.add(context.getString(com.dariusepure.caractivitylog.R.string.formatter_power_hp, hp))
        }
        if (car.engineSize.isNotBlank()) details.add(context.getString(com.dariusepure.caractivitylog.R.string.formatter_engine_size, car.engineSize))
        
        return details.joinToString(" \u00B7 ")
    }

    fun formatDate(date: Date): String = dateFormat.format(date)

    fun getInspectionExpiryText(context: android.content.Context, inspection: VehicleInspection?): String {
        if (inspection == null) return context.getString(com.dariusepure.caractivitylog.R.string.formatter_no_inspection)
        return context.getString(com.dariusepure.caractivitylog.R.string.formatter_inspection_valid_until, formatDate(inspection.expiryDate))
    }

    fun isInspectionExpired(inspection: VehicleInspection?): Boolean {
        return inspection?.expiryDate?.before(Date()) ?: false
    }

    fun formatDimensions(context: android.content.Context, car: Car): String {
        val dims = mutableListOf<String>()
        if (car.length > 0 || car.width > 0 || car.height > 0) {
            dims.add("${car.length}\u00A0x\u00A0${car.width}\u00A0x\u00A0${car.height}\u00A0mm")
        }
        
        return if (dims.isEmpty()) "-" else dims.joinToString("\n")
    }
}

