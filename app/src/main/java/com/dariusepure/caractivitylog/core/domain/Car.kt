package com.dariusepure.caractivitylog.core.domain

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class Car(
    val id: String = "",
    val name: String = "", // Used for Car Title / Nickname
    val licensePlate: String = "",
    val plateCountry: String = "", // Country code (e.g., RO, DE, IT)
    val make: String = "",
    val model: String = "",
    val vin: String = "",
    val year: Int = 0,
    val engineSize: String = "",
    val fuelType: String = "",
    val fuelSystem: String = "",
    val color: String = "",
    val power: Int = 0,
    val powerUnit: String = "hp",
    val torque: Int = 0,
    val engineCode: String = "",
    val engineLayout: String = "",
    val cylinderLayout: String = "",
    val length: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val wheelbase: Int = 0,
    val emissionStandard: String = "",
    val aspiration: String = "",
    val fuelTankCapacity: Double = 0.0,
    val batteryCapacity: Double = 0.0,
    val drivetrain: String = "",
    val gearboxType: String = "",
    val gears: String = "",
    val frontSuspension: String = "",
    val rearSuspension: String = "",
    val frontBrakes: String = "",
    val rearBrakes: String = "",
    val vehicleType: String = "",
    val manufacturingCountry: String = "",
    val topSpeed: Double = 0.0,
    val acceleration0to100: Double = 0.0,
    val fuelConsumptionCombined: Double = 0.0,
    val fuelConsumptionUrban: Double = 0.0,
    val fuelConsumptionExtraUrban: Double = 0.0,
    val co2Emissions: Int = 0,
    val weight: Int = 0,
    val numberOfSeats: Int = 0,
    val numberOfCylinders: Int = 0,
    val valvesPerCylinder: Int = 0,
    val numberOfDoors: Int = 0,
    val bootSpace: Int = 0,
    val tireWidth: Int = 0,
    val tireAspectRatio: Int = 0,
    val tireDiameter: Int = 0,
    val equipments: List<String> = emptyList(),
    val accentColor: Long? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val activityCount: Int = 0,
    val airbags: Int = 0,
    val generation: String = "",
    val engineVariant: String = "",
    val isPendingSync: Boolean = false
)

val Car.displayName: String
    get() {
        val base = if (make.isNotBlank() && model.isNotBlank()) "$make $model".trim() else name.ifBlank { "$make $model".trim() }
        return base.ifBlank { "Unnamed car" }
    }

fun Car.getLocalizedDisplayName(context: android.content.Context): String {
    val base = if (make.isNotBlank() && model.isNotBlank()) "$make $model".trim() else name.ifBlank { "$make $model".trim() }
    if (base.isNotBlank() && base != "Unnamed car") return base
    val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
    val contextToUse = if (!locales.isEmpty) {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locales.get(0))
        context.createConfigurationContext(config)
    } else {
        context
    }
    return contextToUse.getString(com.dariusepure.caractivitylog.R.string.car_unnamed)
}

object CarEquipment {
    const val ABS = "safety_abs"
    const val ESP = "safety_esp"
    const val ASR = "safety_asr"
    const val ISOFIX = "safety_isofix"
}

