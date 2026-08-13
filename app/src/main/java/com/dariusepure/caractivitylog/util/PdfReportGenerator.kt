package com.dariusepure.caractivitylog.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.dariusepure.caractivitylog.ui.cars.europeanCountries
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.Vignette
import com.dariusepure.caractivitylog.ui.common.CarTranslations
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595 // A4 at 72 DPI
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)

    enum class ReportType {
        FULL, TECHNICAL_SHEET, MILEAGE_HISTORY
    }

    fun generateReport(
        context: Context,
        car: Car,
        mileageLogs: List<MileageLog>,
        inspections: List<VehicleInspection>,
        fuelLogs: List<FuelLog>,
        tireSets: List<TireSet>,
        maintenanceLogs: List<Maintenance>,
        insurances: List<Insurance>,
        vignettes: List<Vignette>,
        outputStream: OutputStream,
        reportType: ReportType = ReportType.FULL
    ) {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        
        val currentLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", currentLocale)
        val timeFormat = SimpleDateFormat("HH:mm", currentLocale)

        val accentColorInt = 0xFF1A73E8.toInt() // Default blue
        val accentColorPaint = Paint().apply { color = accentColorInt }
        val textPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        val secondaryTextPaint = Paint().apply { color = Color.DKGRAY; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var yPosition = MARGIN

        fun drawFooter(canvas: Canvas, pageNum: Int) {
            val footerPaint = Paint().apply { 
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }
            val footerY = PAGE_HEIGHT - 30f
            val dateStr = "${dateFormat.format(Date())} ${timeFormat.format(Date())}"
            canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_footer_text, dateStr), MARGIN, footerY, footerPaint)
            
            val pageStr = context.getString(com.dariusepure.caractivitylog.R.string.pdf_page_number, pageNum)
            val bounds = Rect()
            footerPaint.getTextBounds(pageStr, 0, pageStr.length, bounds)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - bounds.width(), footerY, footerPaint)
        }

        fun checkAndCreateNewPage(extraHeight: Float = 20f) {
            if (yPosition + extraHeight > PAGE_HEIGHT - 60f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                yPosition = MARGIN + 20f // Top margin on new pages
            }
        }

        // --- HEADER ---
        textPaint.textSize = 28f
        textPaint.isFakeBoldText = true
        val title = if (car.name.isNotBlank()) "${car.name} (${car.make} ${car.model})" else "${car.make} ${car.model}"
        canvas.drawText(title, MARGIN, yPosition + 25f, textPaint)
        
        yPosition += 40f
        accentColorPaint.strokeWidth = 3f
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, accentColorPaint)
        yPosition += 25f

        // --- TWO COLUMN HELPER ---
        fun drawTwoColumns(items: List<Pair<String, String>>) {
            val colWidth = CONTENT_WIDTH / 2
            textPaint.textSize = 11f
            secondaryTextPaint.textSize = 10f
            
            for (i in items.indices step 2) {
                checkAndCreateNewPage(25f)
                
                // Left Column
                val left = items[i]
                canvas.drawText(left.first, MARGIN, yPosition, secondaryTextPaint)
                canvas.drawText(left.second, MARGIN, yPosition + 14f, textPaint)
                
                // Right Column
                if (i + 1 < items.size) {
                    val right = items[i + 1]
                    canvas.drawText(right.first, MARGIN + colWidth, yPosition, secondaryTextPaint)
                    canvas.drawText(right.second, MARGIN + colWidth, yPosition + 14f, textPaint)
                }
                
                yPosition += 35f
            }
        }

        fun drawSectionHeader(title: String) {
            checkAndCreateNewPage(40f)
            yPosition += 10f
            accentColorPaint.alpha = 40 // Light background for header
            canvas.drawRect(MARGIN - 5f, yPosition - 18f, PAGE_WIDTH - MARGIN + 5f, yPosition + 6f, accentColorPaint)
            accentColorPaint.alpha = 255
            
            val headerPaint = Paint().apply { 
                color = accentColorInt
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(title, MARGIN, yPosition, headerPaint)
            yPosition += 25f
        }

        // 1. Identity
        if (reportType == ReportType.FULL || reportType == ReportType.TECHNICAL_SHEET) {
            val country = europeanCountries.find { it.code == car.plateCountry }
            
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_identity_section))
            drawTwoColumns(listOf(
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_nickname) to car.name,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_make) to car.make,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_model) to car.model,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_year) to car.year.takeIf { it != 0 }?.toString().orEmpty(),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_color) to car.color,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_license_plate) to car.licensePlate,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_country) to CarTranslations.getCountryName(context, car.plateCountry, country?.name ?: car.plateCountry),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vin) to car.vin,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vehicle_type) to CarTranslations.getVehicleTypeLabel(context, car.vehicleType),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_manufacturing_country) to car.manufacturingCountry
            ).filter { it.second.isNotBlank() })

            // 2. Engine & Transmission
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_engine_transmission_section))
            val speedUnit = context.getString(if (country?.usesMiles == true) com.dariusepure.caractivitylog.R.string.pdf_unit_mph else com.dariusepure.caractivitylog.R.string.pdf_unit_kmh)
            val powerUnit = context.getString(if (car.powerUnit.lowercase() == "hp") com.dariusepure.caractivitylog.R.string.pdf_unit_hp else com.dariusepure.caractivitylog.R.string.pdf_unit_kw)
            val consUnit = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_liters_100km)
            
            val engineSpecs = mutableListOf(
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_size) to (if (car.engineSize.isNotBlank()) context.getString(com.dariusepure.caractivitylog.R.string.formatter_engine_size, car.engineSize) else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_type) to CarTranslations.getFuelTypeLabel(context, car.fuelType),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_system) to CarTranslations.getFuelSystemLabel(context, car.fuelSystem),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_aspiration) to CarTranslations.getAspirationLabel(context, car.aspiration),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_power) to (if (car.power > 0) "${car.power} $powerUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_torque) to (if (car.torque > 0) "${car.torque} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_nm)}" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_code) to car.engineCode,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_layout) to CarTranslations.getEngineLayoutLabel(context, car.engineLayout),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_cylinders) to car.numberOfCylinders.takeIf { it != 0 }?.toString().orEmpty(),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_valves_per_cylinder) to car.valvesPerCylinder.takeIf { it != 0 }?.toString().orEmpty(),
                context.getString(com.dariusepure.caractivitylog.R.string.car_valves_label) to (car.numberOfCylinders * car.valvesPerCylinder).takeIf { it > 0 }?.toString().orEmpty(),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_cylinder_layout) to CarTranslations.getCylinderLayoutLabel(context, car.cylinderLayout),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_acceleration) to (if (car.acceleration0to100 > 0) "${car.acceleration0to100} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_seconds)}" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_top_speed) to (if (car.topSpeed > 0) "${car.topSpeed.toInt()} $speedUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_emission_standard) to CarTranslations.getEmissionStandardLabel(context, car.emissionStandard),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_co2) to (if (car.co2Emissions > 0) "${car.co2Emissions} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_co2)}" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_urban_full) to (if (car.fuelConsumptionUrban > 0) "${car.fuelConsumptionUrban} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_extra_urban_full) to (if (car.fuelConsumptionExtraUrban > 0) "${car.fuelConsumptionExtraUrban} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_mixed_full) to (if (car.fuelConsumptionCombined > 0) "${car.fuelConsumptionCombined} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gearbox_type) to CarTranslations.getGearboxTypeLabel(context, car.gearboxType),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gears) to car.gears,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_drivetrain) to CarTranslations.getDrivetrainLabel(context, car.drivetrain),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_suspension) to CarTranslations.getSuspensionLabel(context, car.frontSuspension),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_suspension) to CarTranslations.getSuspensionLabel(context, car.rearSuspension),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_brakes) to CarTranslations.getBrakesLabel(context, car.frontBrakes),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_brakes) to CarTranslations.getBrakesLabel(context, car.rearBrakes)
            )

            drawTwoColumns(engineSpecs.filter { it.second.isNotBlank() })

            // 3. Dimensions & Chassis
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_dimensions_section))
            val mm = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_mm)
            val kg = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_kg)
            val liters = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_liters)
            
            val tireSizeText = if (car.tireWidth > 0 && car.tireAspectRatio > 0 && car.tireDiameter > 0) {
                "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}"
            } else ""

            val dimensionSpecs = mutableListOf(
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_tires) to tireSizeText,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_length) to if (car.length > 0) "${car.length} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_width) to if (car.width > 0) "${car.width} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_height) to if (car.height > 0) "${car.height} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_wheelbase) to if (car.wheelbase > 0) "${car.wheelbase} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_weight) to if (car.weight > 0) "${car.weight} $kg" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_boot_space) to if (car.bootSpace > 0) "${car.bootSpace} $liters" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_seats) to if (car.numberOfSeats > 0) car.numberOfSeats.toString() else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_doors) to if (car.numberOfDoors > 0) car.numberOfDoors.toString() else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_tank_capacity) to if (car.fuelTankCapacity > 0) "${car.fuelTankCapacity} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_liters)}" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_battery_capacity) to if (car.batteryCapacity > 0) "${car.batteryCapacity} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_kwh)}" else ""
            )

            drawTwoColumns(dimensionSpecs.filter { it.second.isNotBlank() })
        }


        // 3. Lists (Single Column, Table Style)
        if (reportType == ReportType.FULL && maintenanceLogs.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_service))
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"
            
            maintenanceLogs.sortedByDescending { it.date }.forEach { log ->
                checkAndCreateNewPage(60f)
                textPaint.textSize = 11f
                canvas.drawText(dateFormat.format(log.date), MARGIN, yPosition, secondaryTextPaint)
                canvas.drawText("${log.km.toInt()} $unit", PAGE_WIDTH - MARGIN - 80f, yPosition, textPaint)
                yPosition += 15f
                canvas.drawText(log.description, MARGIN, yPosition, textPaint)
                yPosition += 14f
                canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_maintenance_category, log.category), MARGIN, yPosition, secondaryTextPaint)
                yPosition += 10f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
            }
        }

        if (reportType == ReportType.FULL && insurances.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_insurance))
            insurances.sortedByDescending { it.date }.forEach { insurance ->
                checkAndCreateNewPage(35f)
                canvas.drawText(dateFormat.format(insurance.date), MARGIN, yPosition, secondaryTextPaint)
                yPosition += 14f
                canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_insurance_entry, insurance.provider, dateFormat.format(insurance.expiryDate)), MARGIN, yPosition, textPaint)
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
            }
        }

        if (reportType == ReportType.FULL && vignettes.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_vignette))
            vignettes.sortedByDescending { it.date }.forEach { vignette ->
                checkAndCreateNewPage(35f)
                canvas.drawText(dateFormat.format(vignette.date), MARGIN, yPosition, secondaryTextPaint)
                yPosition += 14f
                canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_vignette_entry, vignette.country, dateFormat.format(vignette.expiryDate)), MARGIN, yPosition, textPaint)
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
            }
        }

        if (reportType == ReportType.FULL && inspections.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_inspections))
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"
            
            inspections.sortedByDescending { it.date }.forEach { inspection ->
                checkAndCreateNewPage(35f)
                canvas.drawText(dateFormat.format(inspection.date), MARGIN, yPosition, textPaint)
                canvas.drawText("${inspection.mileage.toInt()} $unit", PAGE_WIDTH - MARGIN - 80f, yPosition, textPaint)
                yPosition += 14f
                canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_valid_until, dateFormat.format(inspection.expiryDate)), MARGIN, yPosition, secondaryTextPaint)
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
            }
        }

        if (reportType == ReportType.FULL && fuelLogs.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_fuel))
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"
            
            fuelLogs.sortedByDescending { it.date }.forEach { log ->
                checkAndCreateNewPage(25f)
                canvas.drawText(dateFormat.format(log.date), MARGIN, yPosition, secondaryTextPaint)
                val fullTankTag = if (log.isFullTank) " [${context.getString(com.dariusepure.caractivitylog.R.string.pdf_full_tank_label)}]" else ""
                val fuelText = "${log.liters} L \u00B7 ${log.km.toInt()} $unit$fullTankTag"
                canvas.drawText(fuelText, PAGE_WIDTH - MARGIN - 140f, yPosition, textPaint)
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 18f
            }
        }

        if (reportType == ReportType.FULL && tireSets.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_tire_sets))
            tireSets.sortedByDescending { it.isActive }.forEach { tireSet ->
                checkAndCreateNewPage(35f)
                val activeTag = if (tireSet.isActive) " [${context.getString(com.dariusepure.caractivitylog.R.string.pdf_active_tag).trim()}]" else ""
                val seasonStr = context.getString(tireSet.season.labelRes)
                
                textPaint.isFakeBoldText = tireSet.isActive
                canvas.drawText("${tireSet.brand} ($seasonStr)$activeTag", MARGIN, yPosition, textPaint)
                textPaint.isFakeBoldText = false
                
                yPosition += 14f
                val dotText = when {
                    tireSet.dotWeek != null && tireSet.dotYear != null -> {
                        val weekStr = tireSet.dotWeek.toString().padStart(2, '0')
                        val yearStr = tireSet.dotYear.toString().let { if (it.length > 2) it.takeLast(2) else it.padStart(2, '0') }
                        "$weekStr$yearStr"
                    }
                    tireSet.dotWeek != null -> tireSet.dotWeek.toString().padStart(2, '0')
                    tireSet.dotYear != null -> tireSet.dotYear.toString()
                    else -> "-"
                }
                val specs = "${tireSet.width}/${tireSet.ratio} R${tireSet.diameter} \u00B7 DOT $dotText"
                canvas.drawText(specs, MARGIN, yPosition, secondaryTextPaint)
                
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
            }
        }

        if ((reportType == ReportType.FULL || reportType == ReportType.MILEAGE_HISTORY) && mileageLogs.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_mileage))
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"
            
            mileageLogs.sortedByDescending { it.date }.forEach { log ->
                checkAndCreateNewPage(25f)
                canvas.drawText(dateFormat.format(log.date), MARGIN, yPosition, textPaint)
                canvas.drawText("${log.km.toInt()} $unit", PAGE_WIDTH - MARGIN - 80f, yPosition, textPaint)
                yPosition += 8f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 18f
            }
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}

