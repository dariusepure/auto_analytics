package com.dariusepure.caractivitylog.data.cars

import android.content.Context
import android.util.Log
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Vignette
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStorageHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    private val filesDir = context.filesDir

    private fun getFile(prefix: String, userId: String): File {
        val safeUid = userId.ifBlank { "guest" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(filesDir, "${prefix}_${safeUid}.csv")
    }

    // CARS CSV
    fun loadCars(userId: String): List<Car> {
        val file = getFile("cars", userId)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<Car>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 19) {
                    val car = Car(
                        id = parts[0],
                        name = parts[1].replace(";", ","),
                        licensePlate = parts[2],
                        plateCountry = parts[3],
                        make = parts[4].replace(";", ","),
                        model = parts[5].replace(";", ","),
                        vin = parts[6],
                        year = parts[7].toIntOrNull() ?: 0,
                        engineSize = parts[8],
                        fuelType = parts[9],
                        power = parts[10].toIntOrNull() ?: 0,
                        powerUnit = parts[11],
                        vehicleType = parts[12],
                        gearboxType = parts[13],
                        drivetrain = parts[14],
                        color = parts[15],
                        emissionStandard = parts[16],
                        generation = parts[17].replace(";", ","),
                        engineVariant = parts[18].replace(";", ",")
                    )
                    list.add(car)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading cars CSV", e)
        }
        return list
    }

    fun saveCars(userId: String, cars: List<Car>) {
        val file = getFile("cars", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,name,licensePlate,plateCountry,make,model,vin,year,engineSize,fuelType,power,powerUnit,vehicleType,gearboxType,drivetrain,color,emissionStandard,generation,engineVariant\n")
            cars.forEach { item ->
                val cleanName = item.name.replace(",", ";")
                val cleanMake = item.make.replace(",", ";")
                val cleanModel = item.model.replace(",", ";")
                val cleanGen = item.generation.replace(",", ";")
                val cleanVar = item.engineVariant.replace(",", ";")
                sb.append("${item.id},$cleanName,${item.licensePlate},${item.plateCountry},$cleanMake,$cleanModel,${item.vin},${item.year},${item.engineSize},${item.fuelType},${item.power},${item.powerUnit},${item.vehicleType},${item.gearboxType},${item.drivetrain},${item.color},${item.emissionStandard},$cleanGen,$cleanVar\n")
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing cars CSV", e)
        }
    }

    // INSPECTIONS CSV (id,carId,date,mileage,durationValue,durationUnit,mileageLogId)
    fun loadInspections(userId: String): Map<String, List<VehicleInspection>> {
        val file = getFile("inspections", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<VehicleInspection>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 7) {
                    val id = parts[0]
                    val carId = parts[1]
                    val date = Date(parts[2].toLongOrNull() ?: System.currentTimeMillis())
                    val mileage = parts[3].toDoubleOrNull() ?: 0.0
                    val durationValue = parts[4].toIntOrNull() ?: 1
                    val durationUnit = try { InspectionDurationUnit.valueOf(parts[5]) } catch (e: Exception) { InspectionDurationUnit.YEARS }
                    val mileageLogId = parts[6]

                    val inspection = VehicleInspection(id, date, mileage, durationValue, durationUnit, mileageLogId)
                    result.getOrPut(carId) { mutableListOf() }.add(inspection)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading inspections CSV", e)
        }
        return result
    }

    fun saveInspections(userId: String, allMap: Map<String, List<VehicleInspection>>) {
        val file = getFile("inspections", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,date,mileage,durationValue,durationUnit,mileageLogId\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    sb.append("${item.id},$carId,${item.date.time},${item.mileage},${item.durationValue},${item.durationUnit.name},${item.mileageLogId}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing inspections CSV", e)
        }
    }

    // FUEL LOGS CSV (id,carId,date,km,liters,isFullTank,mileageLogId)
    fun loadFuelLogs(userId: String): Map<String, List<FuelLog>> {
        val file = getFile("fuel_logs", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<FuelLog>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 7) {
                    val id = parts[0]
                    val carId = parts[1]
                    val date = Date(parts[2].toLongOrNull() ?: System.currentTimeMillis())
                    val km = parts[3].toDoubleOrNull() ?: 0.0
                    val liters = parts[4].toDoubleOrNull() ?: 0.0
                    val isFullTank = parts[5].toBooleanStrictOrNull() ?: true
                    val mileageLogId = parts[6]

                    val fuel = FuelLog(id, date, km, liters, isFullTank, mileageLogId)
                    result.getOrPut(carId) { mutableListOf() }.add(fuel)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading fuel logs CSV", e)
        }
        return result
    }

    fun saveFuelLogs(userId: String, allMap: Map<String, List<FuelLog>>) {
        val file = getFile("fuel_logs", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,date,km,liters,isFullTank,mileageLogId\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    sb.append("${item.id},$carId,${item.date.time},${item.km},${item.liters},${item.isFullTank},${item.mileageLogId}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing fuel logs CSV", e)
        }
    }

    // MAINTENANCE CSV (id,carId,date,km,description,mileageLogId,category)
    fun loadMaintenanceLogs(userId: String): Map<String, List<Maintenance>> {
        val file = getFile("maintenance_logs", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<Maintenance>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 7) {
                    val id = parts[0]
                    val carId = parts[1]
                    val date = Date(parts[2].toLongOrNull() ?: System.currentTimeMillis())
                    val km = parts[3].toDoubleOrNull() ?: 0.0
                    val description = parts[4].replace(";", ",")
                    val mileageLogId = parts[5]
                    val category = parts[6]

                    val item = Maintenance(id, date, km, description, mileageLogId, category)
                    result.getOrPut(carId) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading maintenance CSV", e)
        }
        return result
    }

    fun saveMaintenanceLogs(userId: String, allMap: Map<String, List<Maintenance>>) {
        val file = getFile("maintenance_logs", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,date,km,description,mileageLogId,category\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    val cleanDesc = item.description.replace(",", ";")
                    sb.append("${item.id},$carId,${item.date.time},${item.km},$cleanDesc,${item.mileageLogId},${item.category}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing maintenance CSV", e)
        }
    }

    // MILEAGE LOGS CSV (id,carId,km,date)
    fun loadMileageLogs(userId: String): Map<String, List<MileageLog>> {
        val file = getFile("mileage_logs", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<MileageLog>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    val id = parts[0]
                    val carId = parts[1]
                    val km = parts[2].toDoubleOrNull() ?: 0.0
                    val date = Date(parts[3].toLongOrNull() ?: System.currentTimeMillis())

                    val item = MileageLog(id, km, date)
                    result.getOrPut(carId) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading mileage CSV", e)
        }
        return result
    }

    fun saveMileageLogs(userId: String, allMap: Map<String, List<MileageLog>>) {
        val file = getFile("mileage_logs", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,km,date\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    sb.append("${item.id},$carId,${item.km},${item.date.time}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing mileage CSV", e)
        }
    }

    // INSURANCES CSV (id,carId,date,durationValue,durationUnit,provider)
    fun loadInsurances(userId: String): Map<String, List<Insurance>> {
        val file = getFile("insurances", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<Insurance>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val carId = parts[1]
                    val date = Date(parts[2].toLongOrNull() ?: System.currentTimeMillis())
                    val durationValue = parts[3].toIntOrNull() ?: 1
                    val durationUnit = try { InspectionDurationUnit.valueOf(parts[4]) } catch (e: Exception) { InspectionDurationUnit.MONTHS }
                    val provider = parts[5].replace(";", ",")

                    val item = Insurance(id, date, durationValue, durationUnit, provider)
                    result.getOrPut(carId) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading insurances CSV", e)
        }
        return result
    }

    fun saveInsurances(userId: String, allMap: Map<String, List<Insurance>>) {
        val file = getFile("insurances", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,date,durationValue,durationUnit,provider\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    val cleanProvider = item.provider.replace(",", ";")
                    sb.append("${item.id},$carId,${item.date.time},${item.durationValue},${item.durationUnit.name},$cleanProvider\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing insurances CSV", e)
        }
    }

    // VIGNETTES CSV (id,carId,date,durationValue,durationUnit,country)
    fun loadVignettes(userId: String): Map<String, List<Vignette>> {
        val file = getFile("vignettes", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<Vignette>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val carId = parts[1]
                    val date = Date(parts[2].toLongOrNull() ?: System.currentTimeMillis())
                    val durationValue = parts[3].toIntOrNull() ?: 1
                    val durationUnit = try { InspectionDurationUnit.valueOf(parts[4]) } catch (e: Exception) { InspectionDurationUnit.MONTHS }
                    val country = parts[5]

                    val item = Vignette(id, date, durationValue, durationUnit, country)
                    result.getOrPut(carId) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading vignettes CSV", e)
        }
        return result
    }

    fun saveVignettes(userId: String, allMap: Map<String, List<Vignette>>) {
        val file = getFile("vignettes", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,date,durationValue,durationUnit,country\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    sb.append("${item.id},$carId,${item.date.time},${item.durationValue},${item.durationUnit.name},${item.country}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing vignettes CSV", e)
        }
    }

    // TIRE SETS CSV (id,carId,season,brand,width,ratio,diameter,dotWeek,dotYear,isActive)
    fun loadTireSets(userId: String): Map<String, List<TireSet>> {
        val file = getFile("tire_sets", userId)
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<String, MutableList<TireSet>>()
        try {
            file.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 10) {
                    val id = parts[0]
                    val carId = parts[1]
                    val season = try { TireSeason.valueOf(parts[2]) } catch (e: Exception) { TireSeason.SUMMER }
                    val brand = parts[3].replace(";", ",")
                    val width = parts[4].toIntOrNull() ?: 0
                    val ratio = parts[5].toIntOrNull() ?: 0
                    val diameter = parts[6].toIntOrNull() ?: 0
                    val dotWeek = parts[7].toIntOrNull()
                    val dotYear = parts[8].toIntOrNull()
                    val isActive = parts[9].toBooleanStrictOrNull() ?: false

                    val item = TireSet(id, season, brand, width, ratio, diameter, dotWeek, dotYear, isActive)
                    result.getOrPut(carId) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading tire sets CSV", e)
        }
        return result
    }

    fun saveTireSets(userId: String, allMap: Map<String, List<TireSet>>) {
        val file = getFile("tire_sets", userId)
        try {
            val sb = StringBuilder()
            sb.append("id,carId,season,brand,width,ratio,diameter,dotWeek,dotYear,isActive\n")
            allMap.forEach { (carId, list) ->
                list.forEach { item ->
                    val cleanBrand = item.brand.replace(",", ";")
                    sb.append("${item.id},$carId,${item.season.name},$cleanBrand,${item.width},${item.ratio},${item.diameter},${item.dotWeek ?: ""},${item.dotYear ?: ""},${item.isActive}\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing tire sets CSV", e)
        }
    }
}
