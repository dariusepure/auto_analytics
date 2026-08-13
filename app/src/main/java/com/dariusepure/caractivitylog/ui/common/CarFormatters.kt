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
    private const val GALLON_UK_TO_LITER = 4.54609
    private const val MPG_UK_CONSTANT = 282.481

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

    /**
     * Converts consumption to canonical (L/100km).
     * If usesMiles is true, input is MPG (UK).
     */
    fun toCanonicalConsumption(value: Double, usesMiles: Boolean): Double {
        if (value <= 0) return 0.0
        return if (usesMiles) MPG_UK_CONSTANT / value else value
    }

    /**
     * Converts canonical (L/100km) to display unit.
     * If usesMiles is true, returns MPG (UK).
     */
    fun fromCanonicalConsumption(value: Double, usesMiles: Boolean): Double {
        if (value <= 0) return 0.0
        return if (usesMiles) MPG_UK_CONSTANT / value else value
    }

    /**
     * Converts canonical (Liters) to display unit.
     * If usesMiles is true, returns Gallons (UK).
     */
    fun fromCanonicalVolume(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value / GALLON_UK_TO_LITER else value
    }

    /**
     * Converts display volume to canonical (Liters).
     * If usesMiles is true, input is Gallons (UK).
     */
    fun toCanonicalVolume(value: Double, usesMiles: Boolean): Double {
        return if (usesMiles) value * GALLON_UK_TO_LITER else value
    }

    fun calculateConsumption(liters: Double, distKm: Double, usesMiles: Boolean): Double {
        if (distKm <= 0) return 0.0
        val l100 = (liters / distKm) * 100
        return if (usesMiles) MPG_UK_CONSTANT / l100 else l100
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

