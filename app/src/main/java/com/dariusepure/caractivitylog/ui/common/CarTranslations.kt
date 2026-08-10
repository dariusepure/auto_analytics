/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

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
        else -> type
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
        "Twin-Turbo" -> context.getString(R.string.aspiration_twin_turbo)
        "Quad-Turbo" -> context.getString(R.string.aspiration_quad_turbo)
        "Electric" -> context.getString(R.string.aspiration_electric)
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
        "AWD" -> context.getString(R.string.drivetrain_awd)
        "4WD" -> context.getString(R.string.drivetrain_4wd)
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
}

