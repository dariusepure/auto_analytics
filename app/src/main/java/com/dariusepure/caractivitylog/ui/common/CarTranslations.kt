package com.dariusepure.caractivitylog.ui.common

import android.content.Context
import com.dariusepure.caractivitylog.R

object CarTranslations {
    fun getFuelTypeLabel(context: Context, type: String): String = when (type) {
        "Petrol" -> context.getString(R.string.fuel_petrol)
        "Diesel" -> context.getString(R.string.fuel_diesel)
        "Electric" -> context.getString(R.string.fuel_electric)
        "Hybrid" -> context.getString(R.string.fuel_hybrid)
        "LPG" -> context.getString(R.string.fuel_lpg)
        "CNG" -> context.getString(R.string.fuel_cng)
        "Hydrogen" -> context.getString(R.string.fuel_hydrogen)
        else -> type
    }

    fun getColorLabel(context: Context, color: String): String = when (color.lowercase()) {
        "white" -> context.getString(R.string.color_white)
        "black" -> context.getString(R.string.color_black)
        "silver" -> context.getString(R.string.color_silver)
        "gray", "grey" -> context.getString(R.string.color_gray)
        "blue" -> context.getString(R.string.color_blue)
        "red" -> context.getString(R.string.color_red)
        "brown" -> context.getString(R.string.color_brown)
        "green" -> context.getString(R.string.color_green)
        "yellow" -> context.getString(R.string.color_yellow)
        "orange" -> context.getString(R.string.color_orange)
        else -> color
    }

    fun getGearboxTypeLabel(context: Context, type: String): String = when (type) {
        "Manual" -> context.getString(R.string.gearbox_manual)
        "Automatic" -> context.getString(R.string.gearbox_automatic)
        "CVT" -> context.getString(R.string.gearbox_cvt)
        "DCT" -> context.getString(R.string.gearbox_dct)
        "AMT" -> context.getString(R.string.gearbox_amt)
        else -> type
    }

    fun getEngineLayoutLabel(context: Context, layout: String): String = when (layout) {
        "Transverse" -> context.getString(R.string.engine_layout_transverse)
        "Longitudinal" -> context.getString(R.string.engine_layout_longitudinal)
        else -> layout
    }

    fun getCylinderLayoutLabel(context: Context, layout: String): String = when (layout) {
        "Inline" -> context.getString(R.string.cylinder_layout_inline)
        "V" -> context.getString(R.string.cylinder_layout_v)
        "W" -> context.getString(R.string.cylinder_layout_w)
        "Boxer" -> context.getString(R.string.cylinder_layout_boxer)
        else -> layout
    }

    fun getAspirationLabel(context: Context, option: String): String = when (option) {
        "Naturally Aspirated" -> context.getString(R.string.aspiration_naturally_aspirated)
        "Turbocharged" -> context.getString(R.string.aspiration_turbocharged)
        "Supercharged" -> context.getString(R.string.aspiration_supercharged)
        else -> option
    }

    fun getFuelSystemLabel(context: Context, option: String): String = when (option) {
        "Carburetor" -> context.getString(R.string.fuel_system_carburetor)
        "Multi Point Injection" -> context.getString(R.string.fuel_system_multi_point_injection)
        "Direct Injection" -> context.getString(R.string.fuel_system_direct_injection)
        "Injection Pump" -> context.getString(R.string.fuel_system_injection_pump)
        "Pumpe Duse" -> context.getString(R.string.fuel_system_pumpe_duse)
        "Common Rail" -> context.getString(R.string.fuel_system_common_rail)
        else -> option
    }

    fun getSuspensionLabel(context: Context, option: String): String = when (option) {
        "MacPherson" -> context.getString(R.string.suspension_macpherson)
        "Double Wishbone" -> context.getString(R.string.suspension_double_wishbone)
        "Multi-link" -> context.getString(R.string.suspension_multi_link)
        "Torsion Beam" -> context.getString(R.string.suspension_torsion_beam)
        "Solid Axle" -> context.getString(R.string.suspension_solid_axle)
        else -> option
    }

    fun getBrakesLabel(context: Context, option: String): String = when (option) {
        "Ventilated Discs" -> context.getString(R.string.brakes_ventilated_discs)
        "Solid Discs" -> context.getString(R.string.brakes_solid_discs)
        "Drums" -> context.getString(R.string.brakes_drums)
        "Ceramic Discs" -> context.getString(R.string.brakes_ceramic_discs)
        else -> option
    }

    fun getDrivetrainLabel(context: Context, option: String): String = when (option) {
        "FWD" -> context.getString(R.string.drivetrain_fwd)
        "RWD" -> context.getString(R.string.drivetrain_rwd)
        "AWD", "4WD" -> context.getString(R.string.drivetrain_awd)
        else -> option
    }

    fun getEmissionStandardLabel(context: Context, standard: String): String = when (standard) {
        "Non-Euro" -> context.getString(R.string.emission_none)
        "Euro 1" -> context.getString(R.string.emission_euro1)
        "Euro 2" -> context.getString(R.string.emission_euro2)
        "Euro 3" -> context.getString(R.string.emission_euro3)
        "Euro 4" -> context.getString(R.string.emission_euro4)
        "Euro 5" -> context.getString(R.string.emission_euro5)
        "Euro 6" -> context.getString(R.string.emission_euro6)
        else -> standard
    }

    fun getPowerUnitLabel(context: Context, unit: String): String = when (unit.lowercase()) {
        "hp" -> context.getString(R.string.pdf_unit_hp)
        "kw" -> context.getString(R.string.pdf_unit_kw)
        else -> unit
    }

    fun getCountryName(context: Context, countryCode: String, fallbackName: String): String {
        if (countryCode.isBlank()) return fallbackName
        return try {
            val locale = java.util.Locale("", countryCode)
            val currentLocale = context.resources.configuration.locales[0] ?: java.util.Locale.getDefault()
            val displayName = locale.getDisplayCountry(currentLocale)
            if (displayName.isNotBlank() && displayName != countryCode) displayName else fallbackName
        } catch (e: Exception) {
            fallbackName
        }
    }

    fun getVehicleTypeLabel(context: Context, type: String): String = when (type) {
        "Saloon" -> context.getString(R.string.vehicle_type_saloon)
        "Estate" -> context.getString(R.string.vehicle_type_estate)
        "Hatchback" -> context.getString(R.string.vehicle_type_hatchback)
        "MPV" -> context.getString(R.string.vehicle_type_mpv)
        "SUV" -> context.getString(R.string.vehicle_type_suv)
        "Coupe" -> context.getString(R.string.vehicle_type_coupe)
        "Convertible" -> context.getString(R.string.vehicle_type_convertible)
        "Van" -> context.getString(R.string.vehicle_type_van)
        "Pickup" -> context.getString(R.string.vehicle_type_pickup)
        "Liftback" -> context.getString(R.string.vehicle_type_liftback)
        "Fastback" -> context.getString(R.string.vehicle_type_fastback)
        "Targa" -> context.getString(R.string.vehicle_type_targa)
        "Roadster" -> context.getString(R.string.vehicle_type_roadster)
        "Spider" -> context.getString(R.string.vehicle_type_spider)
        "Coupe-Cabriolet" -> context.getString(R.string.vehicle_type_coupe_cabriolet)
        "Shooting Brake" -> context.getString(R.string.vehicle_type_shooting_brake)
        "Crossover" -> context.getString(R.string.vehicle_type_crossover)
        "Minivan" -> context.getString(R.string.vehicle_type_minivan)
        else -> type
    }

    fun getServiceOperationLabel(context: Context, operation: String): String = when (operation) {
        "Oil and Filter Change" -> context.getString(R.string.service_op_oil_filter)
        "Air Filter Replacement" -> context.getString(R.string.service_op_air_filter)
        "Cabin Filter Replacement" -> context.getString(R.string.service_op_cabin_filter)
        "Fuel Filter Replacement" -> context.getString(R.string.service_op_fuel_filter)
        "Brake Pads Replacement" -> context.getString(R.string.service_op_brake_pads)
        "Brake Discs Replacement" -> context.getString(R.string.service_op_brake_discs)
        "Timing Belt / Water Pump Kit" -> context.getString(R.string.service_op_timing_kit)
        "Clutch Kit Replacement" -> context.getString(R.string.service_op_clutch_kit)
        "Battery Replacement" -> context.getString(R.string.service_op_battery)
        "Suspension Overhaul" -> context.getString(R.string.service_op_suspension)
        "Wheel Alignment" -> context.getString(R.string.service_op_alignment)
        "AC Recharge (Freon)" -> context.getString(R.string.service_op_ac_recharge)
        "Spark Plugs Replacement" -> context.getString(R.string.service_op_spark_plugs)
        "Engine Overhaul" -> context.getString(R.string.service_op_engine_overhaul)
        "Injectors Cleaning/Replacement" -> context.getString(R.string.service_op_injectors)
        "Turbocharger Repair/Replacement" -> context.getString(R.string.service_op_turbo)
        "Transmission Oil Change" -> context.getString(R.string.service_op_transmission_oil)
        "Brake Fluid Change" -> context.getString(R.string.service_op_brake_fluid)
        "Coolant (Antifreeze) Change" -> context.getString(R.string.service_op_coolant)
        "DPF / EGR Cleaning" -> context.getString(R.string.service_op_dpf_egr)
        "Accessory Belt Replacement" -> context.getString(R.string.service_op_accessory_belt)
        "Shock Absorbers Replacement" -> context.getString(R.string.service_op_shocks)
        "Steering System Repair" -> context.getString(R.string.service_op_steering)
        "Computer Diagnosis (Tester)" -> context.getString(R.string.service_op_diagnosis)
        "Other (Manual Entry)" -> context.getString(R.string.service_op_other)
        else -> operation
    }

    fun getEquipmentLabel(context: Context, id: String): String {
        val resId = when (id) {
            com.dariusepure.caractivitylog.domain.CarEquipment.ABS -> R.string.equip_abs
            com.dariusepure.caractivitylog.domain.CarEquipment.ESP -> R.string.equip_esp
            com.dariusepure.caractivitylog.domain.CarEquipment.ASR -> R.string.equip_asr
            com.dariusepure.caractivitylog.domain.CarEquipment.ISOFIX -> R.string.equip_isofix
            com.dariusepure.caractivitylog.domain.CarEquipment.LANE_ASSIST -> R.string.equip_lane_assist
            com.dariusepure.caractivitylog.domain.CarEquipment.BLIND_SPOT -> R.string.equip_blind_spot
            com.dariusepure.caractivitylog.domain.CarEquipment.ADAPTIVE_CRUISE -> R.string.equip_adaptive_cruise
            com.dariusepure.caractivitylog.domain.CarEquipment.EMERGENCY_BRAKE -> R.string.equip_emergency_brake
            com.dariusepure.caractivitylog.domain.CarEquipment.AC -> R.string.equip_ac
            com.dariusepure.caractivitylog.domain.CarEquipment.CLIMATE_CONTROL -> R.string.equip_climate_control
            com.dariusepure.caractivitylog.domain.CarEquipment.HEATED_SEATS -> R.string.equip_heated_seats
            com.dariusepure.caractivitylog.domain.CarEquipment.VENTILATED_SEATS -> R.string.equip_ventilated_seats
            com.dariusepure.caractivitylog.domain.CarEquipment.HEATED_STEERING -> R.string.equip_heated_steering
            com.dariusepure.caractivitylog.domain.CarEquipment.LEATHER_INTERIOR -> R.string.equip_leather_interior
            com.dariusepure.caractivitylog.domain.CarEquipment.ELECTRIC_WINDOWS -> R.string.equip_electric_windows
            com.dariusepure.caractivitylog.domain.CarEquipment.POWER_STEERING -> R.string.equip_power_steering
            com.dariusepure.caractivitylog.domain.CarEquipment.CENTRAL_LOCKING -> R.string.equip_central_locking
            com.dariusepure.caractivitylog.domain.CarEquipment.NAVIGATION -> R.string.equip_navigation
            com.dariusepure.caractivitylog.domain.CarEquipment.BLUETOOTH -> R.string.equip_bluetooth
            com.dariusepure.caractivitylog.domain.CarEquipment.CARPLAY_ANDROID_AUTO -> R.string.equip_carplay_android_auto
            com.dariusepure.caractivitylog.domain.CarEquipment.KEYLESS -> R.string.equip_keyless
            com.dariusepure.caractivitylog.domain.CarEquipment.START_STOP -> R.string.equip_start_stop
            com.dariusepure.caractivitylog.domain.CarEquipment.SUNROOF -> R.string.equip_sunroof
            com.dariusepure.caractivitylog.domain.CarEquipment.XENON_LED -> R.string.equip_xenon_led
            com.dariusepure.caractivitylog.domain.CarEquipment.FOG_LIGHTS -> R.string.equip_fog_lights
            com.dariusepure.caractivitylog.domain.CarEquipment.ALLOY_WHEELS -> R.string.equip_alloy_wheels
            com.dariusepure.caractivitylog.domain.CarEquipment.RAIN_SENSORS -> R.string.equip_rain_sensors
            com.dariusepure.caractivitylog.domain.CarEquipment.LIGHT_SENSORS -> R.string.equip_light_sensors
            com.dariusepure.caractivitylog.domain.CarEquipment.PARKING_SENSORS -> R.string.equip_parking_sensors
            com.dariusepure.caractivitylog.domain.CarEquipment.REAR_CAMERA -> R.string.equip_rear_camera
            com.dariusepure.caractivitylog.domain.CarEquipment.CAMERA_360 -> R.string.equip_360_camera
            com.dariusepure.caractivitylog.domain.CarEquipment.PARK_ASSIST -> R.string.equip_park_assist
            else -> null
        }
        return resId?.let { context.getString(it) } ?: id
    }
}

