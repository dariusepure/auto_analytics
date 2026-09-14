package com.dariusepure.caractivitylog.ui.common

import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.VehicleInspection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object CarFormatters {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
        if (car.vehicleType.isNotBlank()) details.add(CarTranslations.getVehicleTypeLabel(context, car.vehicleType))
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

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

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

    fun getBrandLogoResource(make: String): Int? {
        if (make.isBlank()) return null
        
        val normalizedMake = make.lowercase()
            .replace(" ", "-")
            .replace("\u00EB", "e") // ë -> e (Citroën)
        
        return when (normalizedMake) {
            "abarth" -> com.dariusepure.caractivitylog.R.drawable.abarth
            "acura" -> com.dariusepure.caractivitylog.R.drawable.acura
            "alfa-romeo" -> com.dariusepure.caractivitylog.R.drawable.alfa_romeo
            "alpine" -> com.dariusepure.caractivitylog.R.drawable.alpine
            "aston-martin" -> com.dariusepure.caractivitylog.R.drawable.aston_martin
            "audi" -> com.dariusepure.caractivitylog.R.drawable.audi
            "bentley" -> com.dariusepure.caractivitylog.R.drawable.bentley
            "bmw" -> com.dariusepure.caractivitylog.R.drawable.bmw
            "bugatti" -> com.dariusepure.caractivitylog.R.drawable.bugatti
            "buick" -> com.dariusepure.caractivitylog.R.drawable.buick
            "byd" -> com.dariusepure.caractivitylog.R.drawable.byd
            "cadillac" -> com.dariusepure.caractivitylog.R.drawable.cadillac
            "caterham" -> com.dariusepure.caractivitylog.R.drawable.caterham
            "chevrolet" -> com.dariusepure.caractivitylog.R.drawable.chevrolet
            "chrysler" -> com.dariusepure.caractivitylog.R.drawable.chrysler
            "citroen" -> com.dariusepure.caractivitylog.R.drawable.citroen
            "cupra" -> com.dariusepure.caractivitylog.R.drawable.cupra
            "dacia" -> com.dariusepure.caractivitylog.R.drawable.dacia
            "daewoo" -> com.dariusepure.caractivitylog.R.drawable.daewoo
            "daihatsu" -> com.dariusepure.caractivitylog.R.drawable.daihatsu
            "dodge" -> com.dariusepure.caractivitylog.R.drawable.dodge
            "ds" -> com.dariusepure.caractivitylog.R.drawable.ds
            "ferrari" -> com.dariusepure.caractivitylog.R.drawable.ferrari
            "fiat" -> com.dariusepure.caractivitylog.R.drawable.fiat
            "ford" -> com.dariusepure.caractivitylog.R.drawable.ford
            "genesis" -> com.dariusepure.caractivitylog.R.drawable.genesis
            "gmc" -> com.dariusepure.caractivitylog.R.drawable.gmc
            "honda" -> com.dariusepure.caractivitylog.R.drawable.honda
            "hummer" -> com.dariusepure.caractivitylog.R.drawable.hummer
            "hyundai" -> com.dariusepure.caractivitylog.R.drawable.hyundai
            "infiniti" -> com.dariusepure.caractivitylog.R.drawable.infiniti
            "isuzu" -> com.dariusepure.caractivitylog.R.drawable.isuzu
            "jaguar" -> com.dariusepure.caractivitylog.R.drawable.jaguar
            "jeep" -> com.dariusepure.caractivitylog.R.drawable.jeep
            "kia" -> com.dariusepure.caractivitylog.R.drawable.kia
            "koenigsegg" -> com.dariusepure.caractivitylog.R.drawable.koenigsegg
            "lada" -> com.dariusepure.caractivitylog.R.drawable.lada
            "lamborghini" -> com.dariusepure.caractivitylog.R.drawable.lamborghini
            "lancia" -> com.dariusepure.caractivitylog.R.drawable.lancia
            "land-rover" -> com.dariusepure.caractivitylog.R.drawable.land_rover
            "lexus" -> com.dariusepure.caractivitylog.R.drawable.lexus
            "lincoln" -> com.dariusepure.caractivitylog.R.drawable.lincoln
            "lotus" -> com.dariusepure.caractivitylog.R.drawable.lotus
            "lucid" -> com.dariusepure.caractivitylog.R.drawable.lucid
            "maserati" -> com.dariusepure.caractivitylog.R.drawable.maserati
            "maybach" -> com.dariusepure.caractivitylog.R.drawable.maybach
            "mazda" -> com.dariusepure.caractivitylog.R.drawable.mazda
            "mclaren" -> com.dariusepure.caractivitylog.R.drawable.mclaren
            "mercedes-benz" -> com.dariusepure.caractivitylog.R.drawable.mercedes_benz
            "mg" -> com.dariusepure.caractivitylog.R.drawable.mg
            "mini" -> com.dariusepure.caractivitylog.R.drawable.mini
            "mitsubishi" -> com.dariusepure.caractivitylog.R.drawable.mitsubishi
            "morgan" -> com.dariusepure.caractivitylog.R.drawable.morgan
            "nissan" -> com.dariusepure.caractivitylog.R.drawable.nissan
            "opel" -> com.dariusepure.caractivitylog.R.drawable.opel
            "pagani" -> com.dariusepure.caractivitylog.R.drawable.pagani
            "peugeot" -> com.dariusepure.caractivitylog.R.drawable.peugeot
            "polestar" -> com.dariusepure.caractivitylog.R.drawable.polestar
            "porsche" -> com.dariusepure.caractivitylog.R.drawable.porsche
            "ram" -> com.dariusepure.caractivitylog.R.drawable.ram
            "renault" -> com.dariusepure.caractivitylog.R.drawable.renault
            "rimac" -> com.dariusepure.caractivitylog.R.drawable.rimac
            "rolls-royce" -> com.dariusepure.caractivitylog.R.drawable.rolls_royce
            "rover" -> com.dariusepure.caractivitylog.R.drawable.rover
            "saab" -> com.dariusepure.caractivitylog.R.drawable.saab
            "seat" -> com.dariusepure.caractivitylog.R.drawable.seat
            "skoda" -> com.dariusepure.caractivitylog.R.drawable.skoda
            "smart" -> com.dariusepure.caractivitylog.R.drawable.smart
            "ssangyong" -> com.dariusepure.caractivitylog.R.drawable.ssangyong
            "subaru" -> com.dariusepure.caractivitylog.R.drawable.subaru
            "suzuki" -> com.dariusepure.caractivitylog.R.drawable.suzuki
            "tesla" -> com.dariusepure.caractivitylog.R.drawable.tesla
            "toyota" -> com.dariusepure.caractivitylog.R.drawable.toyota
            "triumph" -> com.dariusepure.caractivitylog.R.drawable.triumph
            "tvr" -> com.dariusepure.caractivitylog.R.drawable.tvr
            "vauxhall" -> com.dariusepure.caractivitylog.R.drawable.vauxhall
            "volkswagen" -> com.dariusepure.caractivitylog.R.drawable.volkswagen
            "volvo" -> com.dariusepure.caractivitylog.R.drawable.volvo
            "wartburg" -> com.dariusepure.caractivitylog.R.drawable.wartburg
            else -> null
        }
    }
}

