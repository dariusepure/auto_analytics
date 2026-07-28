package com.dariusepure.caractivitylog.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
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
        drawText("Vehicle History Report", MARGIN, 24f, true)
        yPosition += 20f

        // 1. General Information
        drawSectionTitle("General Information")
        drawField("Name", car.displayName)
        drawField("Make", car.make)
        drawField("Model", car.model)
        drawField("Year", car.year.toString())
        drawField("VIN", car.vin)
        drawField("License Plate", car.licensePlate)
        drawField("Country", car.plateCountry)
        drawField("Color", car.color)
        drawField("Vehicle Type", car.vehicleType)
        drawField("Manufacturing Country", car.manufacturingCountry)

        // 2. Engine & Performance
        drawSectionTitle("Engine & Performance")
        drawField("Engine Size", car.engineSize)
        drawField("Engine Code", car.engineCode)
        drawField("Power", if (car.power > 0) "${car.power} ${car.powerUnit}" else "")
        drawField("Torque", if (car.torque > 0) "${car.torque} Nm" else "")
        drawField("Fuel Type", car.fuelType)
        drawField("Fuel System", car.fuelSystem)
        drawField("Aspiration", car.aspiration)
        drawField("Emission Standard", car.emissionStandard)
        drawField("Top Speed", if (car.topSpeed > 0) "${car.topSpeed} km/h" else "")
        drawField("Cylinders", if (car.numberOfCylinders > 0) car.numberOfCylinders.toString() else "")
        drawField("Valves per Cylinder", if (car.valvesPerCylinder > 0) car.valvesPerCylinder.toString() else "")
        drawField("Engine Layout", car.engineLayout)
        drawField("Cylinder Layout", car.cylinderLayout)

        // 3. Dimensions & Weight
        drawSectionTitle("Dimensions & Weight")
        drawField("Length", if (car.length > 0) "${car.length} mm" else "")
        drawField("Width", if (car.width > 0) "${car.width} mm" else "")
        drawField("Height", if (car.height > 0) "${car.height} mm" else "")
        drawField("Wheelbase", if (car.wheelbase > 0) "${car.wheelbase} mm" else "")
        drawField("Track Width", if (car.trackWidth > 0) "${car.trackWidth} mm" else "")
        drawField("Weight", if (car.weight > 0) "${car.weight} kg" else "")
        drawField("Boot Space", if (car.bootSpace > 0) "${car.bootSpace} L" else "")
        drawField("Fuel Tank Capacity", if (car.fuelTankCapacity > 0) "${car.fuelTankCapacity} L" else "")
        drawField("Battery Capacity", if (car.batteryCapacity > 0) "${car.batteryCapacity} kWh" else "")

        // 4. Drivetrain & Chassis
        drawSectionTitle("Drivetrain & Chassis")
        drawField("Drivetrain", car.drivetrain)
        drawField("Gearbox Type", car.gearboxType)
        drawField("Gears", car.gears)
        drawField("Front Suspension", car.frontSuspension)
        drawField("Rear Suspension", car.rearSuspension)
        drawField("Front Brakes", car.frontBrakes)
        drawField("Rear Brakes", car.rearBrakes)

        // 5. Wheels & Interior
        drawSectionTitle("Wheels & Interior")
        if (car.tireWidth > 0) {
            drawField("Tires", "${car.tireWidth}/${car.tireAspectRatio} R${car.tireDiameter}")
        }
        drawField("Number of Seats", if (car.numberOfSeats > 0) car.numberOfSeats.toString() else "")
        drawField("Number of Doors", if (car.numberOfDoors > 0) car.numberOfDoors.toString() else "")

        // 6. Mileage History
        if (mileageLogs.isNotEmpty()) {
            drawSectionTitle("Mileage History")
            mileageLogs.sortedByDescending { it.date }.forEach { log ->
                drawText("${dateFormat.format(log.date)}: ${log.km.toInt()} km", MARGIN + 20f, 12f)
            }
        }

        // 7. Inspections
        if (inspections.isNotEmpty()) {
            drawSectionTitle("Vehicle Inspections (ITP)")
            inspections.sortedByDescending { it.date }.forEach { inspection ->
                drawText("${dateFormat.format(inspection.date)} - Mileage: ${inspection.mileage.toInt()} km", MARGIN + 20f, 12f, true)
                drawText("  Valid until: ${dateFormat.format(inspection.expiryDate)}", MARGIN + 20f, 12f)
                yPosition += 5f
            }
        }

        // 8. Fuel Logs
        if (fuelLogs.isNotEmpty()) {
            drawSectionTitle("Fuel Consumption")
            fuelLogs.sortedByDescending { it.date }.forEach { log ->
                drawText("${dateFormat.format(log.date)}: ${log.liters}L at ${log.km.toInt()} km", MARGIN + 20f, 12f)
            }
        }

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
