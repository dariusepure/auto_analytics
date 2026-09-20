package com.dariusepure.caractivitylog.domain

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
        val base = if (name.isNotBlank()) name else "$make $model".trim()
        val withGen = if (generation.isNotBlank()) "$base ($generation)" else base
        return if (engineVariant.isNotBlank()) "$withGen $engineVariant".trim() else withGen.ifBlank { "Unnamed car" }
    }

object CarEquipment {
    const val ABS = "safety_abs"
    const val ESP = "safety_esp"
    const val ASR = "safety_asr"
    const val ISOFIX = "safety_isofix"
    const val LANE_ASSIST = "safety_lane_assist"
    const val BLIND_SPOT = "safety_blind_spot"
    const val ADAPTIVE_CRUISE = "safety_adaptive_cruise"
    const val EMERGENCY_BRAKE = "safety_emergency_brake"

    const val AC = "comfort_ac"
    const val CLIMATE_CONTROL = "comfort_climate_control"
    const val HEATED_SEATS = "comfort_heated_seats"
    const val VENTILATED_SEATS = "comfort_ventilated_seats"
    const val HEATED_STEERING = "comfort_heated_steering"
    const val LEATHER_INTERIOR = "comfort_leather_interior"
    const val ELECTRIC_WINDOWS = "comfort_electric_windows"
    const val POWER_STEERING = "comfort_power_steering"
    const val CENTRAL_LOCKING = "comfort_central_locking"

    const val NAVIGATION = "tech_navigation"
    const val BLUETOOTH = "tech_bluetooth"
    const val CARPLAY_ANDROID_AUTO = "tech_carplay_android_auto"
    const val KEYLESS = "tech_keyless"
    const val START_STOP = "tech_start_stop"

    const val SUNROOF = "ext_sunroof"
    const val XENON_LED = "ext_xenon_led"
    const val FOG_LIGHTS = "ext_fog_lights"
    const val ALLOY_WHEELS = "ext_alloy_wheels"
    const val RAIN_SENSORS = "ext_rain_sensors"
    const val LIGHT_SENSORS = "ext_light_sensors"

    const val PARKING_SENSORS = "park_sensors"
    const val REAR_CAMERA = "park_rear_camera"
    const val CAMERA_360 = "park_360_camera"
    const val PARK_ASSIST = "park_assist"
}

