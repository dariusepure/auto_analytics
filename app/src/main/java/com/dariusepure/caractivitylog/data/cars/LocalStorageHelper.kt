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
import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class LocalStorageHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    private val filesDir = context.filesDir

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun getSafeUid(userId: String): String {
        return if (userId.isBlank() || userId == "guest" || userId == "local_guest_user") {
            "guest"
        } else {
            userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }

    private fun getFile(prefix: String, userId: String): File {
        val safeUid = getSafeUid(userId)
        val file = File(filesDir, "${prefix}_${safeUid}.csv")
        if (!file.exists() && safeUid == "guest") {
            val legacyGuestFile = File(filesDir, "${prefix}_local_guest_user.csv")
            if (legacyGuestFile.exists()) return legacyGuestFile
        }
        return file
    }

    private fun getJsonFile(prefix: String, userId: String): File {
        val safeUid = getSafeUid(userId)
        val file = File(filesDir, "${prefix}_${safeUid}.json")
        if (!file.exists() && safeUid == "guest") {
            val legacyGuestFile = File(filesDir, "${prefix}_local_guest_user.json")
            if (legacyGuestFile.exists()) return legacyGuestFile
        }
        return file
    }

    // CARS JSON / CSV
    fun loadCars(userId: String): List<Car> {
        val jsonFile = getJsonFile("cars", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteCar.serializer()), jsonStr)
                    return remoteList.map { it.fromRemote() }
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading cars JSON", e)
            }
        }

        val csvFile = getFile("cars", userId)
        if (!csvFile.exists()) return emptyList()
        val list = mutableListOf<Car>()
        try {
            csvFile.readLines().drop(1).forEach { line ->
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
                        engineVariant = parts[18].replace(";", ","),
                        isPendingSync = parts.getOrNull(19)?.toBooleanStrictOrNull() ?: false
                    )
                    list.add(car)
                }
            }
            if (list.isNotEmpty()) {
                saveCars(userId, list)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading cars CSV", e)
        }
        return list
    }

    fun saveCars(userId: String, cars: List<Car>) {
        val jsonFile = getJsonFile("cars", userId)
        try {
            val remoteList = cars.map { it.toRemote() }
            val jsonStr = json.encodeToString(ListSerializer(RemoteCar.serializer()), remoteList)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing cars JSON", e)
        }
    }

    // INSPECTIONS JSON (with CSV fallback/migration)
    fun loadInspections(userId: String): Map<String, List<VehicleInspection>> {
        val jsonFile = getJsonFile("inspections", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteVehicleInspection.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<VehicleInspection>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading inspections JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveInspections(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading inspections CSV", e)
        }
        return result
    }

    fun saveInspections(userId: String, allMap: Map<String, List<VehicleInspection>>) {
        val jsonFile = getJsonFile("inspections", userId)
        try {
            val list = mutableListOf<RemoteVehicleInspection>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteVehicleInspection.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing inspections JSON", e)
        }
    }

    // FUEL LOGS JSON (with CSV fallback/migration)
    fun loadFuelLogs(userId: String): Map<String, List<FuelLog>> {
        val jsonFile = getJsonFile("fuel_logs", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteFuelLog.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<FuelLog>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading fuel logs JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveFuelLogs(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading fuel logs CSV", e)
        }
        return result
    }

    fun saveFuelLogs(userId: String, allMap: Map<String, List<FuelLog>>) {
        val jsonFile = getJsonFile("fuel_logs", userId)
        try {
            val list = mutableListOf<RemoteFuelLog>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteFuelLog.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing fuel logs JSON", e)
        }
    }

    // MAINTENANCE JSON (with CSV fallback/migration)
    fun loadMaintenanceLogs(userId: String): Map<String, List<Maintenance>> {
        val jsonFile = getJsonFile("maintenance_logs", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteMaintenance.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<Maintenance>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading maintenance JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveMaintenanceLogs(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading maintenance CSV", e)
        }
        return result
    }

    fun saveMaintenanceLogs(userId: String, allMap: Map<String, List<Maintenance>>) {
        val jsonFile = getJsonFile("maintenance_logs", userId)
        try {
            val list = mutableListOf<RemoteMaintenance>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteMaintenance.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing maintenance JSON", e)
        }
    }

    // MILEAGE LOGS JSON (with CSV fallback/migration)
    fun loadMileageLogs(userId: String): Map<String, List<MileageLog>> {
        val jsonFile = getJsonFile("mileage_logs", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteMileageLog.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<MileageLog>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading mileage JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveMileageLogs(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading mileage CSV", e)
        }
        return result
    }

    fun saveMileageLogs(userId: String, allMap: Map<String, List<MileageLog>>) {
        val jsonFile = getJsonFile("mileage_logs", userId)
        try {
            val list = mutableListOf<RemoteMileageLog>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteMileageLog.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing mileage JSON", e)
        }
    }

    // INSURANCES JSON (with CSV fallback/migration)
    fun loadInsurances(userId: String): Map<String, List<Insurance>> {
        val jsonFile = getJsonFile("insurances", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteInsurance.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<Insurance>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading insurances JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveInsurances(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading insurances CSV", e)
        }
        return result
    }

    fun saveInsurances(userId: String, allMap: Map<String, List<Insurance>>) {
        val jsonFile = getJsonFile("insurances", userId)
        try {
            val list = mutableListOf<RemoteInsurance>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteInsurance.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing insurances JSON", e)
        }
    }

    // VIGNETTES JSON (with CSV fallback/migration)
    fun loadVignettes(userId: String): Map<String, List<Vignette>> {
        val jsonFile = getJsonFile("vignettes", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteVignette.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<Vignette>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading vignettes JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveVignettes(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading vignettes CSV", e)
        }
        return result
    }

    fun saveVignettes(userId: String, allMap: Map<String, List<Vignette>>) {
        val jsonFile = getJsonFile("vignettes", userId)
        try {
            val list = mutableListOf<RemoteVignette>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteVignette.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing vignettes JSON", e)
        }
    }

    // TIRE SETS JSON (with CSV fallback/migration)
    fun loadTireSets(userId: String): Map<String, List<TireSet>> {
        val jsonFile = getJsonFile("tire_sets", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteTireSet.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<TireSet>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.fromRemote())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading tire sets JSON", e)
            }
        }

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
            if (result.isNotEmpty()) {
                saveTireSets(userId, result)
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error reading tire sets CSV", e)
        }
        return result
    }

    fun saveTireSets(userId: String, allMap: Map<String, List<TireSet>>) {
        val jsonFile = getJsonFile("tire_sets", userId)
        try {
            val list = mutableListOf<RemoteTireSet>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(item.toRemote().copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteTireSet.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing tire sets JSON", e)
        }
    }

    // DIAGNOSIS MESSAGES JSON
    fun loadDiagnosisMessages(userId: String): Map<String, List<ChatMessage>> {
        val jsonFile = getJsonFile("diagnosis", userId)
        if (jsonFile.exists()) {
            try {
                val jsonStr = jsonFile.readText()
                if (jsonStr.isNotBlank()) {
                    val remoteList = json.decodeFromString(ListSerializer(RemoteChatMessage.serializer()), jsonStr)
                    val map = mutableMapOf<String, MutableList<ChatMessage>>()
                    remoteList.forEach { remote ->
                        map.getOrPut(remote.carId) { mutableListOf() }.add(remote.toChatMessage())
                    }
                    return map
                }
            } catch (e: Exception) {
                Log.e("LocalStorageHelper", "Error reading diagnosis JSON", e)
            }
        }
        return emptyMap()
    }

    fun saveDiagnosisMessages(userId: String, allMap: Map<String, List<ChatMessage>>) {
        val jsonFile = getJsonFile("diagnosis", userId)
        try {
            val list = mutableListOf<RemoteChatMessage>()
            allMap.forEach { (carId, items) ->
                items.forEach { item ->
                    list.add(RemoteChatMessage.fromChatMessage(item).copy(carId = carId))
                }
            }
            val jsonStr = json.encodeToString(ListSerializer(RemoteChatMessage.serializer()), list)
            jsonFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error writing diagnosis JSON", e)
        }
    }

    fun clearLocalData(userId: String) {
        try {
            val safeUid = getSafeUid(userId)
            listOf("cars", "inspections", "fuel_logs", "maintenance_logs", "mileage_logs", "insurances", "vignettes", "tire_sets", "diagnosis").forEach { prefix ->
                getJsonFile(prefix, safeUid).delete()
                getFile(prefix, safeUid).delete()
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error clearing local data", e)
        }
    }

    fun clearAllLocalData() {
        try {
            listOf("cars", "inspections", "fuel_logs", "maintenance_logs", "mileage_logs", "insurances", "vignettes", "tire_sets", "diagnosis").forEach { prefix ->
                filesDir.listFiles { file -> file.name.startsWith("${prefix}_") }?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e("LocalStorageHelper", "Error clearing all local data", e)
        }
    }
}
