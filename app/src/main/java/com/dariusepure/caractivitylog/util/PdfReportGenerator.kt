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
    private const val MARGIN = 40f
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

        val accentColorInt = Color.BLACK
        val accentColorPaint = Paint().apply { color = accentColorInt; isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true; textSize = 10f }
        val secondaryTextPaint = Paint().apply { color = Color.DKGRAY; isAntiAlias = true; textSize = 9f }
        val headerPaint = Paint().apply { color = Color.BLACK; isFakeBoldText = true; isAntiAlias = true; textSize = 13f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; isAntiAlias = true }
        
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var yPosition = MARGIN

        fun drawFooter(canvas: Canvas, pageNum: Int) {
            val footerPaint = Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true }
            val footerY = PAGE_HEIGHT - 25f
            val dateStr = "${dateFormat.format(Date())} ${timeFormat.format(Date())}"
            canvas.drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_footer_text, dateStr), MARGIN, footerY, footerPaint)
            
            val pageStr = context.getString(com.dariusepure.caractivitylog.R.string.pdf_page_number, pageNum)
            val bounds = Rect()
            footerPaint.getTextBounds(pageStr, 0, pageStr.length, bounds)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - bounds.width(), footerY, footerPaint)
        }

        fun checkAndCreateNewPage(extraHeight: Float = 20f) {
            if (yPosition + extraHeight > PAGE_HEIGHT - 50f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                yPosition = MARGIN + 10f
            }
        }

        fun drawSectionHeader(title: String) {
            checkAndCreateNewPage(45f)
            yPosition += 15f
            accentColorPaint.alpha = 25
            canvas.drawRoundRect(MARGIN - 5f, yPosition - 18f, PAGE_WIDTH - MARGIN + 5f, yPosition + 6f, 4f, 4f, accentColorPaint)
            accentColorPaint.alpha = 255
            canvas.drawText(title.uppercase(), MARGIN + 5f, yPosition, headerPaint)
            yPosition += 25f
        }

        // --- GRID HELPER (3 COLUMNS) ---
        fun drawThreeColumns(items: List<Pair<String, String>>) {
            val colWidth = CONTENT_WIDTH / 3
            secondaryTextPaint.textSize = 8.5f
            textPaint.textSize = 10.5f
            textPaint.isFakeBoldText = true
            
            for (i in items.indices step 3) {
                checkAndCreateNewPage(35f)
                
                // Col 1
                canvas.drawText(items[i].first, MARGIN, yPosition, secondaryTextPaint)
                canvas.drawText(items[i].second, MARGIN, yPosition + 13f, textPaint)
                
                // Col 2
                if (i + 1 < items.size) {
                    canvas.drawText(items[i + 1].first, MARGIN + colWidth, yPosition, secondaryTextPaint)
                    canvas.drawText(items[i + 1].second, MARGIN + colWidth, yPosition + 13f, textPaint)
                }
                
                // Col 3
                if (i + 2 < items.size) {
                    canvas.drawText(items[i + 2].first, MARGIN + colWidth * 2, yPosition, secondaryTextPaint)
                    canvas.drawText(items[i + 2].second, MARGIN + colWidth * 2, yPosition + 13f, textPaint)
                }
                
                yPosition += 32f
            }
            textPaint.isFakeBoldText = false
        }

        // --- TABLE HELPER ---
        fun drawTableHeader(columns: List<Pair<String, Float>>) { // Title to Weight (0-1)
            checkAndCreateNewPage(25f)
            secondaryTextPaint.textSize = 9f
            secondaryTextPaint.isFakeBoldText = true
            var currentX = MARGIN
            columns.forEach { (title, weight) ->
                canvas.drawText(title, currentX, yPosition, secondaryTextPaint)
                currentX += weight * CONTENT_WIDTH
            }
            secondaryTextPaint.isFakeBoldText = false
            yPosition += 6f
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 15f
        }

        fun drawTableRow(data: List<Pair<String, Float>>) {
            checkAndCreateNewPage(20f)
            textPaint.textSize = 10f
            var currentX = MARGIN
            data.forEach { (value, weight) ->
                canvas.drawText(value, currentX, yPosition, textPaint)
                currentX += weight * CONTENT_WIDTH
            }
            yPosition += 18f
        }

        // --- START REPORT ---
        // Header
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = true
        val title = if (car.name.isNotBlank()) "${car.name} (${car.make} ${car.model})" else "${car.make} ${car.model}"
        val titleWidth = textPaint.measureText(title)
        canvas.drawText(title, (PAGE_WIDTH - titleWidth) / 2f, yPosition + 20f, textPaint)
        textPaint.isFakeBoldText = false
        
        yPosition += 35f
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, accentColorPaint.apply { strokeWidth = 2f })
        yPosition += 20f

        if (reportType == ReportType.FULL || reportType == ReportType.TECHNICAL_SHEET) {
            val country = europeanCountries.find { it.code == car.plateCountry }
            
            // 1. Identity
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_identity_section))
            drawThreeColumns(listOf(
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_nickname) to car.name,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_make) to car.make,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_model) to car.model,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_year) to car.year.takeIf { it != 0 }?.toString().orEmpty(),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_color) to car.color,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vin) to car.vin,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vehicle_type) to CarTranslations.getVehicleTypeLabel(context, car.vehicleType),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_manufacturing_country) to car.manufacturingCountry
            ).filter { it.second.isNotBlank() })

            // 2. Engine & Performance
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
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_mixed_full) to (if (car.fuelConsumptionCombined > 0) "${car.fuelConsumptionCombined} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_urban_full) to (if (car.fuelConsumptionUrban > 0) "${car.fuelConsumptionUrban} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.car_consumption_extra_urban_full) to (if (car.fuelConsumptionExtraUrban > 0) "${car.fuelConsumptionExtraUrban} $consUnit" else ""),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gearbox_type) to CarTranslations.getGearboxTypeLabel(context, car.gearboxType),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gears) to car.gears,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_drivetrain) to CarTranslations.getDrivetrainLabel(context, car.drivetrain),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_suspension) to CarTranslations.getSuspensionLabel(context, car.frontSuspension),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_suspension) to CarTranslations.getSuspensionLabel(context, car.rearSuspension),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_brakes) to CarTranslations.getBrakesLabel(context, car.frontBrakes),
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_brakes) to CarTranslations.getBrakesLabel(context, car.rearBrakes)
            )
            drawThreeColumns(engineSpecs.filter { it.second.isNotBlank() })

            // 3. Dimensions
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_dimensions_section))
            val mm = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_mm)
            val kg = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_kg)
            val litersUnit = context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_liters)
            val tireSizeText = if (car.tireWidth > 0 && car.tireAspectRatio > 0 && car.tireDiameter > 0) "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}" else ""

            val dimensionSpecs = mutableListOf(
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_tires) to tireSizeText,
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_length) to if (car.length > 0) "${car.length} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_width) to if (car.width > 0) "${car.width} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_height) to if (car.height > 0) "${car.height} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_wheelbase) to if (car.wheelbase > 0) "${car.wheelbase} $mm" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_weight) to if (car.weight > 0) "${car.weight} $kg" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_boot_space) to if (car.bootSpace > 0) "${car.bootSpace} $litersUnit" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_seats) to if (car.numberOfSeats > 0) car.numberOfSeats.toString() else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_doors) to if (car.numberOfDoors > 0) car.numberOfDoors.toString() else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_tank_capacity) to if (car.fuelTankCapacity > 0) "${car.fuelTankCapacity} $litersUnit" else "",
                context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_battery_capacity) to if (car.batteryCapacity > 0) "${car.batteryCapacity} ${context.getString(com.dariusepure.caractivitylog.R.string.pdf_unit_kwh)}" else ""
            )
            drawThreeColumns(dimensionSpecs.filter { it.second.isNotBlank() })

            // 4. Safety
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.car_safety_section))
            val yes = context.getString(com.dariusepure.caractivitylog.R.string.status_ok)
            val no = context.getString(com.dariusepure.caractivitylog.R.string.common_none)
            val safetySpecs = listOf(
                context.getString(com.dariusepure.caractivitylog.R.string.car_abs_label) to if (car.hasAbs) yes else no,
                context.getString(com.dariusepure.caractivitylog.R.string.car_esp_label) to if (car.hasEsp) yes else no,
                context.getString(com.dariusepure.caractivitylog.R.string.car_airbags_label) to if (car.airbags > 0) car.airbags.toString() else no
            )
            drawThreeColumns(safetySpecs)
        }

        // --- LISTS SECTION ---
        if (reportType == ReportType.FULL) {
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"

            // Maintenance
            if (maintenanceLogs.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_service))
                maintenanceLogs.sortedByDescending { it.date }.forEach { log ->
                    checkAndCreateNewPage(50f)
                    textPaint.isFakeBoldText = true
                    canvas.drawText(log.description, MARGIN, yPosition, textPaint)
                    canvas.drawText("${log.km.toInt()} $unit", PAGE_WIDTH - MARGIN - 80f, yPosition, textPaint)
                    textPaint.isFakeBoldText = false
                    yPosition += 13f
                    canvas.drawText("${dateFormat.format(log.date)} \u2022 ${log.category}", MARGIN, yPosition, secondaryTextPaint)
                    yPosition += 8f
                    canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                    yPosition += 18f
                }
            }

            // Insurance
            if (insurances.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_insurance))
                drawTableHeader(listOf(
                    context.getString(com.dariusepure.caractivitylog.R.string.common_date) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.insurance_provider_label) to 0.45f,
                    context.getString(com.dariusepure.caractivitylog.R.string.common_expires) to 0.3f
                ))
                insurances.sortedByDescending { it.date }.forEach { 
                    drawTableRow(listOf(
                        dateFormat.format(it.date) to 0.25f,
                        it.provider to 0.45f,
                        dateFormat.format(it.expiryDate) to 0.3f
                    ))
                }
                yPosition += 10f
            }

            // Vignette
            if (vignettes.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_vignette))
                drawTableHeader(listOf(
                    context.getString(com.dariusepure.caractivitylog.R.string.common_date) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.vignette_country_label) to 0.45f,
                    context.getString(com.dariusepure.caractivitylog.R.string.common_expires) to 0.3f
                ))
                vignettes.sortedByDescending { it.date }.forEach { 
                    drawTableRow(listOf(
                        dateFormat.format(it.date) to 0.25f,
                        it.country to 0.45f,
                        dateFormat.format(it.expiryDate) to 0.3f
                    ))
                }
                yPosition += 10f
            }

            // ITP
            if (inspections.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_inspections))
                drawTableHeader(listOf(
                    context.getString(com.dariusepure.caractivitylog.R.string.common_date) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.common_mileage) to 0.35f,
                    context.getString(com.dariusepure.caractivitylog.R.string.common_expires) to 0.4f
                ))
                inspections.sortedByDescending { it.date }.forEach { 
                    drawTableRow(listOf(
                        dateFormat.format(it.date) to 0.25f,
                        "${it.mileage.toInt()} $unit" to 0.35f,
                        dateFormat.format(it.expiryDate) to 0.4f
                    ))
                }
                yPosition += 10f
            }

            // Fuel
            if (fuelLogs.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_fuel))
                drawTableHeader(listOf(
                    context.getString(com.dariusepure.caractivitylog.R.string.common_date) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.fuel_liters_label) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.common_mileage) to 0.25f,
                    context.getString(com.dariusepure.caractivitylog.R.string.fuel_full_tank_label) to 0.25f
                ))
                fuelLogs.sortedByDescending { it.date }.forEach { log ->
                    drawTableRow(listOf(
                        dateFormat.format(log.date) to 0.25f,
                        "${log.liters} L" to 0.25f,
                        "${log.km.toInt()} $unit" to 0.25f,
                        (if (log.isFullTank) "\u2713" else "-") to 0.25f
                    ))
                }
                yPosition += 10f
            }

            // Tires
            if (tireSets.isNotEmpty()) {
                drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_tire_sets))
                tireSets.sortedByDescending { it.isActive }.forEach { tireSet ->
                    checkAndCreateNewPage(45f)
                    val activeTag = if (tireSet.isActive) " [${context.getString(com.dariusepure.caractivitylog.R.string.pdf_active_tag).trim()}]" else ""
                    textPaint.isFakeBoldText = tireSet.isActive
                    canvas.drawText("${tireSet.brand} (${context.getString(tireSet.season.labelRes)})$activeTag", MARGIN, yPosition, textPaint)
                    textPaint.isFakeBoldText = false
                    yPosition += 13f
                    val dotText = if (tireSet.dotWeek != null && tireSet.dotYear != null) "${tireSet.dotWeek.toString().padStart(2, '0')}${tireSet.dotYear.toString().takeLast(2)}" else "-"
                    canvas.drawText("${tireSet.width}/${tireSet.ratio} R${tireSet.diameter} \u2022 DOT $dotText \u2022 ${if (tireSet.isActive) context.getString(com.dariusepure.caractivitylog.R.string.tire_on_vehicle) else context.getString(com.dariusepure.caractivitylog.R.string.tire_in_storage)}", MARGIN, yPosition, secondaryTextPaint)
                    yPosition += 8f
                    canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                    yPosition += 18f
                }
            }
        }

        // Mileage History (Always at end or separate)
        if ((reportType == ReportType.FULL || reportType == ReportType.MILEAGE_HISTORY) && mileageLogs.isNotEmpty()) {
            drawSectionHeader(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_mileage))
            val country = europeanCountries.find { it.code == car.plateCountry }
            val unit = if (country?.usesMiles == true) "mi" else "km"
            
            drawTableHeader(listOf(
                context.getString(com.dariusepure.caractivitylog.R.string.common_date) to 0.5f,
                context.getString(com.dariusepure.caractivitylog.R.string.common_mileage) to 0.5f
            ))
            mileageLogs.sortedByDescending { it.date }.forEach { log ->
                drawTableRow(listOf(
                    dateFormat.format(log.date) to 0.5f,
                    "${log.km.toInt()} $unit" to 0.5f
                ))
            }
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
