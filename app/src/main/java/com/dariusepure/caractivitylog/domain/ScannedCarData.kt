package com.dariusepure.caractivitylog.domain

import com.dariusepure.caractivitylog.util.FlexibleDoubleSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ScannedMileageEntry(
    @Serializable(with = FlexibleDoubleSerializer::class)
    val km: Double?,
    val date: String? = null // Format: YYYY-MM-DD
)

@Serializable
data class ScannedCarData(
    val make: String? = null,
    val model: String? = null,
    val vin: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val year: Double? = null,
    val fuelType: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val engineSize: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val power: Double? = null,
    val powerUnit: String? = "hp",
    @Serializable(with = FlexibleDoubleSerializer::class)
    val torque: Double? = null,
    val color: String? = null,
    val registrationPlate: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val numberOfSeats: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val numberOfDoors: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val weight: Double? = null,
    val engineCode: String? = null,
    val emissionStandard: String? = null,
    val gearboxType: String? = null,
    val gears: String? = null,
    val drivetrain: String? = null,
    val engineLayout: String? = null,
    val cylinderLayout: String? = null,
    val aspiration: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val fuelTankCapacity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val topSpeed: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val acceleration0to100: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val fuelConsumptionCombined: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val co2Emissions: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val mileage: Double? = null,
    val mileageHistory: List<ScannedMileageEntry>? = null
)

