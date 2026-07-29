package com.dariusepure.caractivitylog.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.dariusepure.caractivitylog.ui.cars.europeanCountries
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.displayName
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfReportGenerator {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_SPACING = 20f

    fun generateReport(
        context: Context,
        car: Car,
        mileageLogs: List<MileageLog>,
        inspections: List<VehicleInspection>,
        fuelLogs: List<FuelLog>,
        tireSets: List<TireSet>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        val paint = Paint()
        var yPosition = MARGIN

        fun checkAndCreateNewPage(extraHeight: Float = 0f) {
            if (yPosition + extraHeight > PAGE_HEIGHT - MARGIN) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                yPosition = MARGIN
            }
        }

        fun drawText(text: String, x: Float, size: Float, isBold: Boolean = false) {
            paint.textSize = size
            paint.isFakeBoldText = isBold
            checkAndCreateNewPage(size)
            canvas.drawText(text, x, yPosition, paint)
            yPosition += size + 5f
        }

        fun drawSectionTitle(title: String) {
            yPosition += 10f
            drawText(title, MARGIN, 18f, true)
            yPosition += 5f
        }

        fun drawField(label: String, value: String) {
            if (value.isNotBlank() && value != "0" && value != "0.0") {
                drawText("$label: $value", MARGIN + 20f, 12f)
            }
        }

        // Title
        drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_title), MARGIN, 24f, true)
        yPosition += 20f

        // 1. General Information
        drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_general))
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_name), car.displayName)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_make), car.make)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_model), car.model)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_year), car.year.toString())
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vin), car.vin)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_license_plate), car.licensePlate)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_country), car.plateCountry)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_color), car.color)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_vehicle_type), car.vehicleType)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_manufacturing_country), car.manufacturingCountry)

        // 2. Engine & Performance
        drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_engine))
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_size), car.engineSize)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_code), car.engineCode)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_power), if (car.power > 0) "${car.power} ${car.powerUnit}" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_torque), if (car.torque > 0) "${car.torque} Nm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_type), car.fuelType)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_system), car.fuelSystem)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_aspiration), car.aspiration)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_emission_standard), car.emissionStandard)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_top_speed), if (car.topSpeed > 0) "${car.topSpeed} km/h" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_cylinders), if (car.numberOfCylinders > 0) car.numberOfCylinders.toString() else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_valves_per_cylinder), if (car.valvesPerCylinder > 0) car.valvesPerCylinder.toString() else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_engine_layout), car.engineLayout)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_cylinder_layout), car.cylinderLayout)

        // 3. Dimensions & Weight
        drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_dimensions))
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_length), if (car.length > 0) "${car.length} mm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_width), if (car.width > 0) "${car.width} mm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_height), if (car.height > 0) "${car.height} mm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_wheelbase), if (car.wheelbase > 0) "${car.wheelbase} mm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_track_width), if (car.trackWidth > 0) "${car.trackWidth} mm" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_weight), if (car.weight > 0) "${car.weight} kg" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_boot_space), if (car.bootSpace > 0) "${car.bootSpace} L" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_fuel_tank_capacity), if (car.fuelTankCapacity > 0) "${car.fuelTankCapacity} L" else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_battery_capacity), if (car.batteryCapacity > 0) "${car.batteryCapacity} kWh" else "")

        // 4. Drivetrain & Chassis
        drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_drivetrain))
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_drivetrain), car.drivetrain)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gearbox_type), car.gearboxType)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_gears), car.gears)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_suspension), car.frontSuspension)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_suspension), car.rearSuspension)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_front_brakes), car.frontBrakes)
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_rear_brakes), car.rearBrakes)

        // 5. Wheels & Interior
        drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_wheels))
        if (car.tireWidth > 0) {
            drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_tires), "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}")
        }
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_seats), if (car.numberOfSeats > 0) car.numberOfSeats.toString() else "")
        drawField(context.getString(com.dariusepure.caractivitylog.R.string.pdf_field_doors), if (car.numberOfDoors > 0) car.numberOfDoors.toString() else "")

        // 6. Mileage History
        if (mileageLogs.isNotEmpty()) {
            drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_mileage))
            mileageLogs.sortedByDescending { it.date }.forEach { log ->
                val country = europeanCountries.find { it.code == car.plateCountry }
                val unit = if (country?.usesMiles == true) "mi" else "km"
                drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_mileage_entry, dateFormat.format(log.date), log.km.toInt(), unit), MARGIN + 20f, 12f)
            }
        }

        // 7. Inspections
        if (inspections.isNotEmpty()) {
            drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_inspections))
            inspections.sortedByDescending { it.date }.forEach { inspection ->
                val country = europeanCountries.find { it.code == car.plateCountry }
                val unit = if (country?.usesMiles == true) "mi" else "km"
                drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_inspection_entry, dateFormat.format(inspection.date), inspection.mileage.toInt(), unit), MARGIN + 20f, 12f, true)
                drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_valid_until, dateFormat.format(inspection.expiryDate)), MARGIN + 20f, 12f)
                yPosition += 5f
            }
        }

        // 8. Fuel Logs
        if (fuelLogs.isNotEmpty()) {
            drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_fuel))
            fuelLogs.sortedByDescending { it.date }.forEach { log ->
                val country = europeanCountries.find { it.code == car.plateCountry }
                val unit = if (country?.usesMiles == true) "mi" else "km"
                drawText(context.getString(com.dariusepure.caractivitylog.R.string.pdf_fuel_entry, dateFormat.format(log.date), log.liters, log.km.toInt(), unit), MARGIN + 20f, 12f)
            }
        }

        // 9. Tire Sets
        if (tireSets.isNotEmpty()) {
            drawSectionTitle(context.getString(com.dariusepure.caractivitylog.R.string.pdf_section_tire_sets))
            tireSets.sortedByDescending { it.isActive }.forEach { tireSet ->
                val activeTag = if (tireSet.isActive) " " + context.getString(com.dariusepure.caractivitylog.R.string.pdf_active_tag) else ""
                val seasonStr = context.getString(tireSet.season.labelRes)
                
                val text = context.getString(
                    com.dariusepure.caractivitylog.R.string.pdf_tire_set_entry,
                    tireSet.brand,
                    tireSet.model,
                    seasonStr,
                    tireSet.width,
                    tireSet.ratio,
                    tireSet.diameter,
                    tireSet.dot.ifBlank { "-" }
                ) + activeTag
                
                drawText(text, MARGIN + 20f, 12f, tireSet.isActive)
                if (tireSet.storageLocation.isNotBlank()) {
                    drawText("${context.getString(com.dariusepure.caractivitylog.R.string.tire_storage_label)}: ${tireSet.storageLocation}", MARGIN + 40f, 10f)
                }
                yPosition += 5f
            }
        }

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
