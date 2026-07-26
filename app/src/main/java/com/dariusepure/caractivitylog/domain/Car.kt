package com.dariusepure.caractivitylog.domain

import java.util.Date

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
    val trackWidth: Int = 0,
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
    val weight: Int = 0,
    val numberOfSeats: Int = 0,
    val numberOfCylinders: Int = 0,
    val valvesPerCylinder: Int = 0,
    val numberOfDoors: Int = 0,
    val bootSpace: Int = 0,
    val tireWidth: Int = 0,
    val tireAspectRatio: Int = 0,
    val tireDiameter: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val activityCount: Int = 0,
    val deleted: Boolean = false,
    val deletedAt: Date? = null,
    val isSynced: Boolean = true
)

val Car.displayName: String
    get() = name.ifBlank { "$make $model".trim() }.ifBlank { "Unnamed car" }
