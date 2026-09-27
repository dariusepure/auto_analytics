package com.dariusepure.caractivitylog.data.local

import android.content.Context
import android.util.Log
import com.dariusepure.caractivitylog.data.cars.LocalStorageHelper
import com.dariusepure.caractivitylog.data.local.entities.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvToRoomMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localStorageHelper: LocalStorageHelper,
    private val database: AppDatabase
) {
    suspend fun migrateIfNeeded() {
        try {
            val filesDir = context.filesDir
            val csvFiles = filesDir.listFiles { _, name -> name.endsWith(".csv") } ?: emptyArray()
            if (csvFiles.isEmpty()) return

            Log.d("CsvToRoomMigrator", "Found ${csvFiles.size} CSV files to migrate to Room")

            val uids = csvFiles.mapNotNull { file ->
                val name = file.name
                val prefix = name.substringBefore("_")
                val uid = name.substringAfter("${prefix}_").substringBeforeLast(".csv")
                uid.ifBlank { null }
            }.toSet()

            for (uid in uids) {
                val cars = localStorageHelper.loadCars(uid)
                if (cars.isNotEmpty()) {
                    database.carDao().insertCars(cars.map { it.toEntity(uid) })
                }

                val inspectionsMap = localStorageHelper.loadInspections(uid)
                inspectionsMap.forEach { (carId, list) ->
                    database.vehicleInspectionDao().insertInspections(list.map { it.toEntity(carId) })
                }

                val fuelMap = localStorageHelper.loadFuelLogs(uid)
                fuelMap.forEach { (carId, list) ->
                    database.fuelLogDao().insertFuelLogs(list.map { it.toEntity(carId) })
                }

                val maintenanceMap = localStorageHelper.loadMaintenanceLogs(uid)
                maintenanceMap.forEach { (carId, list) ->
                    database.maintenanceDao().insertMaintenanceLogs(list.map { it.toEntity(carId) })
                }

                val mileageMap = localStorageHelper.loadMileageLogs(uid)
                mileageMap.forEach { (carId, list) ->
                    database.mileageLogDao().insertMileageLogs(list.map { it.toEntity(carId) })
                }

                val insuranceMap = localStorageHelper.loadInsurances(uid)
                insuranceMap.forEach { (carId, list) ->
                    database.insuranceDao().insertInsurances(list.map { it.toEntity(carId) })
                }

                val vignetteMap = localStorageHelper.loadVignettes(uid)
                vignetteMap.forEach { (carId, list) ->
                    database.vignetteDao().insertVignettes(list.map { it.toEntity(carId) })
                }

                val tireMap = localStorageHelper.loadTireSets(uid)
                tireMap.forEach { (carId, list) ->
                    database.tireSetDao().insertTireSets(list.map { it.toEntity(carId) })
                }
            }

            csvFiles.forEach { file ->
                file.delete()
            }
            Log.d("CsvToRoomMigrator", "Successfully migrated CSV data to Room SQLite database and cleaned up CSV files")
        } catch (e: Exception) {
            Log.e("CsvToRoomMigrator", "Error migrating CSV to Room", e)
        }
    }
}
