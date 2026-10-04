package com.dariusepure.caractivitylog.data.cars

import android.util.Log
import com.dariusepure.caractivitylog.data.auth.AuthEvent
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.CarReport
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val localStorageHelper: LocalStorageHelper
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val carsCache = MutableStateFlow<List<Car>>(emptyList())
    private val mileageLogsCache = MutableStateFlow<Map<String, List<MileageLog>>>(emptyMap())
    private val inspectionsCache = MutableStateFlow<Map<String, List<VehicleInspection>>>(emptyMap())
    private val insurancesCache = MutableStateFlow<Map<String, List<Insurance>>>(emptyMap())
    private val vignettesCache = MutableStateFlow<Map<String, List<Vignette>>>(emptyMap())
    private val tireSetsCache = MutableStateFlow<Map<String, List<TireSet>>>(emptyMap())
    private val fuelLogsCache = MutableStateFlow<Map<String, List<FuelLog>>>(emptyMap())
    private val maintenanceLogsCache = MutableStateFlow<Map<String, List<Maintenance>>>(emptyMap())
    private val reportsCache = MutableStateFlow<Map<String, List<CarReport>>>(emptyMap())
    private val diagnosisCache = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    private var carsListenerRegistration: ListenerRegistration? = null

    init {
        repositoryScope.launch {
            authRepository.userId.collect { uid ->
                val activeUid = uid ?: "guest"
                switchUserCache(activeUid)
                if (uid != null && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    syncAllAccountData(uid)
                }
            }
        }
    }

    private suspend fun switchUserCache(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                val localCars = try { localStorageHelper.loadCars(uid) } catch (e: Exception) { emptyList() }
                carsCache.value = localCars
                inspectionsCache.value = try { localStorageHelper.loadInspections(uid) } catch (e: Exception) { emptyMap() }
                fuelLogsCache.value = try { localStorageHelper.loadFuelLogs(uid) } catch (e: Exception) { emptyMap() }
                maintenanceLogsCache.value = try { localStorageHelper.loadMaintenanceLogs(uid) } catch (e: Exception) { emptyMap() }
                mileageLogsCache.value = try { localStorageHelper.loadMileageLogs(uid) } catch (e: Exception) { emptyMap() }
                insurancesCache.value = try { localStorageHelper.loadInsurances(uid) } catch (e: Exception) { emptyMap() }
                vignettesCache.value = try { localStorageHelper.loadVignettes(uid) } catch (e: Exception) { emptyMap() }
                tireSetsCache.value = try { localStorageHelper.loadTireSets(uid) } catch (e: Exception) { emptyMap() }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error switching user cache: ${e.message}")
            }
        }
    }

    private fun <T> updateMapCache(
        cache: MutableStateFlow<Map<String, List<T>>>,
        carId: String,
        updateBlock: (List<T>) -> List<T>
    ) {
        val currentMap = cache.value.toMutableMap()
        val currentList = currentMap[carId] ?: emptyList()
        currentMap[carId] = updateBlock(currentList)
        cache.value = currentMap
    }

    private fun updateCarsCache(updateBlock: (List<Car>) -> List<Car>) {
        val uid = getUidSafe()
        val updated = updateBlock(carsCache.value)
        carsCache.value = updated
        repositoryScope.launch(Dispatchers.IO) {
            try {
                localStorageHelper.saveCars(uid, updated)
            } catch (e: Exception) {}
        }
    }

    fun getCarsFromCache(): List<Car> = carsCache.value
    fun getInspectionsFromCache(carId: String): List<VehicleInspection> = inspectionsCache.value[carId] ?: emptyList()
    fun getFuelLogsFromCache(carId: String): List<FuelLog> = fuelLogsCache.value[carId] ?: emptyList()
    fun getMaintenanceLogsFromCache(carId: String): List<Maintenance> = maintenanceLogsCache.value[carId] ?: emptyList()
    fun getMileageLogsFromCache(carId: String): List<MileageLog> = mileageLogsCache.value[carId] ?: emptyList()
    fun getInsurancesFromCache(carId: String): List<Insurance> = insurancesCache.value[carId] ?: emptyList()
    fun getVignettesFromCache(carId: String): List<Vignette> = vignettesCache.value[carId] ?: emptyList()
    fun getTireSetsFromCache(carId: String): List<TireSet> = tireSetsCache.value[carId] ?: emptyList()

    val cars: Flow<List<Car>> = carsCache.asStateFlow()

    private fun syncAllAccountData(uid: String) {
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return

        carsListenerRegistration?.remove()

        repositoryScope.launch(Dispatchers.IO) {
            try {
                val email = authRepository.currentUserEmail
                authRepository.ensureProfileExists(uid, email ?: "")
            } catch (e: Exception) {}
        }

        carsListenerRegistration = firestore.collection("users").document(uid).collection("cars")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CarRepository", "Firebase sync failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allRemoteCars = mutableListOf<Car>()
                    val carDocRefs = mutableMapOf<String, DocumentReference>()

                    for (doc in snapshot.documents) {
                        try {
                            val car = doc.toCar()
                            if (allRemoteCars.none { it.id == car.id }) {
                                allRemoteCars.add(car)
                                carDocRefs[car.id] = doc.reference
                            }
                        } catch (e: Exception) {
                            Log.e("CarRepository", "Error parsing car: ${e.message}")
                        }
                    }

                    carsCache.value = allRemoteCars
                    repositoryScope.launch(Dispatchers.IO) {
                        try {
                            localStorageHelper.saveCars(uid, allRemoteCars)
                        } catch (e: Exception) {}
                    }

                    allRemoteCars.forEach { car ->
                        fetchCarLogs(uid, car.id, carDocRefs[car.id] ?: firestore.collection("users").document(uid).collection("cars").document(car.id))
                    }
                }
            }
    }

    private fun fetchCarLogs(uid: String, carId: String, carDocRef: DocumentReference) {
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val insSnap = carDocRef.collection("inspections").get().await()
                val ins = insSnap.documents.mapNotNull { it.toVehicleInspection() }.sortedByDescending { it.date }
                if (ins.isNotEmpty()) updateMapCache(inspectionsCache, carId) { ins }

                val fuelSnap = carDocRef.collection("fuel_logs").get().await()
                val fuels = fuelSnap.documents.mapNotNull { it.toFuelLog() }.sortedByDescending { it.date }
                if (fuels.isNotEmpty()) updateMapCache(fuelLogsCache, carId) { fuels }

                val maintSnap = carDocRef.collection("maintenance_logs").get().await()
                val maint = maintSnap.documents.mapNotNull { it.toMaintenance() }.sortedByDescending { it.date }
                if (maint.isNotEmpty()) updateMapCache(maintenanceLogsCache, carId) { maint }

                val mileageSnap = carDocRef.collection("mileage_logs").get().await()
                val mileage = mileageSnap.documents.mapNotNull { it.toMileageLog() }.sortedByDescending { it.date }
                if (mileage.isNotEmpty()) updateMapCache(mileageLogsCache, carId) { mileage }

                val insuSnap = carDocRef.collection("insurances").get().await()
                val insu = insuSnap.documents.mapNotNull { it.toInsurance() }.sortedByDescending { it.date }
                if (insu.isNotEmpty()) updateMapCache(insurancesCache, carId) { insu }

                val vigSnap = carDocRef.collection("vignettes").get().await()
                val vig = vigSnap.documents.mapNotNull { it.toVignette() }.sortedByDescending { it.date }
                if (vig.isNotEmpty()) updateMapCache(vignettesCache, carId) { vig }

                val tiresSnap = carDocRef.collection("tire_sets").get().await()
                val tires = tiresSnap.documents.mapNotNull { it.toTireSet() }.sortedByDescending { it.isActive }
                if (tires.isNotEmpty()) updateMapCache(tireSetsCache, carId) { tires }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching logs for $carId: ${e.message}")
            }
        }
    }

    fun refresh() {
        val uid = getUidSafe()
        if (uid != "guest" && uid != AuthRepository.GUEST_UID) {
            syncAllAccountData(uid)
        }
    }

    private fun getUidSafe(): String {
        return authRepository.getUserId() ?: "guest"
    }

    suspend fun createCar(car: Car) {
        val item = if (car.id.isBlank()) car.copy(id = UUID.randomUUID().toString()) else car
        val uid = getUidSafe()

        updateCarsCache { list -> (list.filterNot { it.id == item.id } + item) }

        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val email = authRepository.currentUserEmail ?: ""
                    authRepository.ensureProfileExists(uid, email)

                    val remoteCar = item.toRemote().copy(userId = uid)
                    firestore.collection("users").document(uid).collection("cars").document(remoteCar.id).set(remoteCar).await()
                    Log.d("CarRepository", "createCar saved to Firebase successfully")
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error saving car to Firebase: ${e.message}", e)
                }
            }
        }
    }

    suspend fun updateCar(car: Car) {
        createCar(car)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCarFlow(carId: String): Flow<Car?> = cars.flatMapLatest { list ->
        val car = list.find { it.id == carId }
        if (car != null) {
            flowOf<Car?>(car)
        } else {
            flow {
                emit(getCar(carId))
            }
        }
    }

    suspend fun getCar(carId: String): Car? {
        val cached = try { carsCache.value.find { it.id == carId } } catch (e: Exception) { null }
        if (cached != null) return cached

        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return null

        return try {
            val doc = firestore.collection("users").document(uid).collection("cars").document(carId).get().await()
            doc.toCar()
        } catch (e: Exception) {
            Log.e("CarRepository", "Error getting car $carId: ${e.message}")
            null
        }
    }

    suspend fun isVinDuplicate(vin: String, excludeCarId: String?): Boolean {
        return try {
            val currentCars = carsCache.value
            currentCars.any { it.vin.equals(vin.trim(), ignoreCase = true) && it.id != excludeCarId }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCar(carId: String) {
        updateCarsCache { list -> list.filterNot { it.id == carId } }

        val uid = getUidSafe()
        repositoryScope.launch(Dispatchers.IO) {
            try {
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error deleting car $carId: ${e.message}")
            }
        }
    }

    // MILEAGE LOGS
    fun getMileageLogs(carId: String): Flow<List<MileageLog>> {
        fetchSubCollectionRemote(carId, "mileage_logs", mileageLogsCache) { it.toMileageLog() }
        return mileageLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addMileageLog: ${e.message}")
                }
            }
        }
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        addMileageLog(carId, log)
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        updateMapCache(mileageLogsCache, carId) { list -> list.filterNot { it.id == logId } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(logId).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteMileageLog: ${e.message}")
                }
            }
        }
    }

    // INSPECTIONS (ITP)
    fun getInspections(carId: String): Flow<List<VehicleInspection>> {
        fetchSubCollectionRemote(carId, "inspections", inspectionsCache) { it.toVehicleInspection() }
        return inspectionsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addInspection(carId: String, inspection: VehicleInspection) {
        val item = if (inspection.id.isBlank()) inspection.copy(id = UUID.randomUUID().toString()) else inspection
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addInspection: ${e.message}")
                }
            }
        }
    }

    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        addInspection(carId, inspection)
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        updateMapCache(inspectionsCache, carId) { list -> list.filterNot { it.id == inspection.id } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(inspection.id).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteInspection: ${e.message}")
                }
            }
        }
    }

    // INSURANCES (RCA/CASCO)
    fun getInsurances(carId: String): Flow<List<Insurance>> {
        fetchSubCollectionRemote(carId, "insurances", insurancesCache) { it.toInsurance() }
        return insurancesCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addInsurance(carId: String, insurance: Insurance) {
        val item = if (insurance.id.isBlank()) insurance.copy(id = UUID.randomUUID().toString()) else insurance
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addInsurance: ${e.message}")
                }
            }
        }
    }

    suspend fun updateInsurance(carId: String, insurance: Insurance) {
        addInsurance(carId, insurance)
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        updateMapCache(insurancesCache, carId) { list -> list.filterNot { it.id == insuranceId } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(insuranceId).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteInsurance: ${e.message}")
                }
            }
        }
    }

    // VIGNETTES
    fun getVignettes(carId: String): Flow<List<Vignette>> {
        fetchSubCollectionRemote(carId, "vignettes", vignettesCache) { it.toVignette() }
        return vignettesCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addVignette(carId: String, vignette: Vignette) {
        val item = if (vignette.id.isBlank()) vignette.copy(id = UUID.randomUUID().toString()) else vignette
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addVignette: ${e.message}")
                }
            }
        }
    }

    suspend fun updateVignette(carId: String, vignette: Vignette) {
        addVignette(carId, vignette)
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        updateMapCache(vignettesCache, carId) { list -> list.filterNot { it.id == vignetteId } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(vignetteId).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteVignette: ${e.message}")
                }
            }
        }
    }

    // TIRE SETS
    fun getTireSets(carId: String): Flow<List<TireSet>> {
        fetchSubCollectionRemote(carId, "tire_sets", tireSetsCache) { it.toTireSet() }
        return tireSetsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addTireSet(carId: String, tireSet: TireSet) {
        val item = if (tireSet.id.isBlank()) tireSet.copy(id = UUID.randomUUID().toString()) else tireSet
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.isActive }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addTireSet: ${e.message}")
                }
            }
        }
    }

    suspend fun updateTireSet(carId: String, tireSet: TireSet) {
        addTireSet(carId, tireSet)
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        updateMapCache(tireSetsCache, carId) { list -> list.filterNot { it.id == tireSetId } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(tireSetId).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteTireSet: ${e.message}")
                }
            }
        }
    }

    // DIAGNOSIS CHAT
    fun getDiagnosisMessages(carId: String): Flow<List<ChatMessage>> {
        return diagnosisCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        updateMapCache(diagnosisCache, carId) { list -> list + message }
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        updateMapCache(diagnosisCache, carId) { emptyList() }
    }

    // FUEL LOGS
    fun getFuelLogs(carId: String): Flow<List<FuelLog>> {
        fetchSubCollectionRemote(carId, "fuel_logs", fuelLogsCache) { it.toFuelLog() }
        return fuelLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addFuelLog(carId: String, log: FuelLog) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addFuelLog: ${e.message}")
                }
            }
        }
    }

    suspend fun updateFuelLog(carId: String, log: FuelLog) {
        addFuelLog(carId, log)
    }

    suspend fun deleteFuelLog(carId: String, log: FuelLog) {
        updateMapCache(fuelLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(log.id).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteFuelLog: ${e.message}")
                }
            }
        }
    }

    // MAINTENANCE LOGS
    fun getMaintenanceLogs(carId: String): Flow<List<Maintenance>> {
        fetchSubCollectionRemote(carId, "maintenance_logs", maintenanceLogsCache) { it.toMaintenance() }
        return maintenanceLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addMaintenanceLog(carId: String, log: Maintenance) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addMaintenanceLog: ${e.message}")
                }
            }
        }
    }

    suspend fun updateMaintenanceLog(carId: String, log: Maintenance) {
        addMaintenanceLog(carId, log)
    }

    suspend fun deleteMaintenanceLog(carId: String, log: Maintenance) {
        updateMapCache(maintenanceLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(log.id).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteMaintenanceLog: ${e.message}")
                }
            }
        }
    }

    // REPORTS
    fun getCarReports(carId: String): Flow<List<CarReport>> {
        return reportsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addCarReport(carId: String, report: CarReport) {
        val item = if (report.id.isBlank()) report.copy(id = UUID.randomUUID().toString()) else report
        updateMapCache(reportsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
    }

    suspend fun deleteCarReport(carId: String, reportId: String) {
        updateMapCache(reportsCache, carId) { list -> list.filterNot { it.id == reportId } }
    }

    private fun <T> fetchSubCollectionRemote(
        carId: String,
        collectionName: String,
        cache: MutableStateFlow<Map<String, List<T>>>,
        mapper: (DocumentSnapshot) -> T
    ) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection(collectionName).get().await()
                val results = snap.documents.mapNotNull { doc ->
                    try { mapper(doc) } catch (e: Exception) { null }
                }
                updateMapCache(cache, carId) { results }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching $collectionName for $carId: ${e.message}")
            }
        }
    }
}

// ==========================================
// Robust DocumentSnapshot Extension Functions
// ==========================================

fun DocumentSnapshot.toCar(): Car {
    return Car(
        id = id,
        name = safeString("name"),
        licensePlate = safeString("licensePlate"),
        plateCountry = safeString("plateCountry", "RO"),
        make = safeString("make"),
        model = safeString("model"),
        vin = safeString("vin"),
        year = safeInt("year"),
        engineSize = safeString("engineSize"),
        fuelType = safeString("fuelType"),
        fuelSystem = safeString("fuelSystem"),
        color = safeString("color"),
        power = safeInt("power"),
        powerUnit = safeString("powerUnit", "hp"),
        torque = safeInt("torque"),
        engineCode = safeString("engineCode"),
        engineLayout = safeString("engineLayout"),
        cylinderLayout = safeString("cylinderLayout"),
        length = safeInt("length"),
        width = safeInt("width"),
        height = safeInt("height"),
        wheelbase = safeInt("wheelbase"),
        emissionStandard = safeString("emissionStandard"),
        aspiration = safeString("aspiration"),
        fuelTankCapacity = safeDouble("fuelTankCapacity"),
        batteryCapacity = safeDouble("batteryCapacity"),
        drivetrain = safeString("drivetrain"),
        gearboxType = safeString("gearboxType"),
        gears = safeString("gears"),
        frontSuspension = safeString("frontSuspension"),
        rearSuspension = safeString("rearSuspension"),
        frontBrakes = safeString("frontBrakes"),
        rearBrakes = safeString("rearBrakes"),
        vehicleType = safeString("vehicleType"),
        manufacturingCountry = safeString("manufacturingCountry"),
        topSpeed = safeDouble("topSpeed"),
        acceleration0to100 = safeDouble("acceleration0to100"),
        fuelConsumptionCombined = safeDouble("fuelConsumptionCombined"),
        fuelConsumptionUrban = safeDouble("fuelConsumptionUrban"),
        fuelConsumptionExtraUrban = safeDouble("fuelConsumptionExtraUrban"),
        co2Emissions = safeInt("co2Emissions"),
        weight = safeInt("weight"),
        numberOfSeats = safeInt("numberOfSeats"),
        numberOfCylinders = safeInt("numberOfCylinders"),
        valvesPerCylinder = safeInt("valvesPerCylinder"),
        numberOfDoors = safeInt("numberOfDoors"),
        bootSpace = safeInt("bootSpace"),
        tireWidth = safeInt("tireWidth"),
        tireAspectRatio = safeInt("tireAspectRatio"),
        tireDiameter = safeInt("tireDiameter"),
        equipments = safeStringList("equipments"),
        accentColor = safeLongOrNull("accentColor"),
        createdAt = safeDate("createdAt", Date()),
        updatedAt = safeDate("updatedAt", Date()),
        activityCount = safeInt("activityCount"),
        airbags = safeInt("airbags"),
        generation = safeString("generation"),
        engineVariant = safeString("engineVariant"),
        isPendingSync = false
    )
}

fun DocumentSnapshot.toMileageLog(): MileageLog {
    return MileageLog(
        id = id,
        km = safeDouble("km", safeDouble("mileage", 0.0)),
        date = safeDate("date", Date())
    )
}

fun DocumentSnapshot.toVehicleInspection(): VehicleInspection {
    val unitStr = safeString("durationUnit", "YEARS")
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.YEARS
    }
    return VehicleInspection(
        id = id,
        date = safeDate("date", Date()),
        mileage = safeDouble("mileage", safeDouble("km", 0.0)),
        durationValue = safeInt("durationValue", 1),
        durationUnit = durationUnitEnum,
        mileageLogId = safeString("mileageLogId")
    )
}

fun DocumentSnapshot.toInsurance(): Insurance {
    val unitStr = safeString("durationUnit", "MONTHS")
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.MONTHS
    }
    return Insurance(
        id = id,
        date = safeDate("date", Date()),
        durationValue = safeInt("durationValue", 6),
        durationUnit = durationUnitEnum,
        provider = safeString("provider")
    )
}

fun DocumentSnapshot.toVignette(): Vignette {
    val unitStr = safeString("durationUnit", "MONTHS")
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.MONTHS
    }
    return Vignette(
        id = id,
        date = safeDate("date", Date()),
        durationValue = safeInt("durationValue", 1),
        durationUnit = durationUnitEnum,
        country = safeString("country")
    )
}

fun DocumentSnapshot.toTireSet(): TireSet {
    val seasonStr = safeString("season", "SUMMER")
    val seasonEnum = try {
        TireSeason.valueOf(seasonStr)
    } catch (e: Exception) {
        TireSeason.SUMMER
    }
    return TireSet(
        id = id,
        season = seasonEnum,
        brand = safeString("brand"),
        width = safeInt("width"),
        ratio = safeInt("ratio"),
        diameter = safeInt("diameter"),
        dotWeek = safeIntOrNull("dotWeek"),
        dotYear = safeIntOrNull("dotYear"),
        isActive = safeBoolean("isActive", false)
    )
}

fun DocumentSnapshot.toFuelLog(): FuelLog {
    return FuelLog(
        id = id,
        date = safeDate("date", Date()),
        km = safeDouble("km", safeDouble("mileage", 0.0)),
        liters = safeDouble("liters"),
        isFullTank = safeBoolean("isFullTank", true),
        mileageLogId = safeString("mileageLogId")
    )
}

fun DocumentSnapshot.toMaintenance(): Maintenance {
    return Maintenance(
        id = id,
        date = safeDate("date", Date()),
        km = safeDouble("km", safeDouble("mileage", 0.0)),
        description = safeString("description"),
        mileageLogId = safeString("mileageLogId"),
        category = safeString("category", "General")
    )
}

fun DocumentSnapshot.toCarReport(): CarReport {
    return CarReport(
        id = id,
        carId = safeString("carId"),
        fileName = safeString("fileName", safeString("file_name")),
        date = safeDate("date", Date())
    )
}

fun DocumentSnapshot.toChatMessage(): ChatMessage {
    return ChatMessage(
        text = safeString("text"),
        isUser = safeBoolean("isUser", false),
        timestamp = safeLong("timestamp", System.currentTimeMillis())
    )
}

// ==========================================
// Safe Field Reading Helpers
// ==========================================

private fun String.camelToSnake(): String {
    return this.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun DocumentSnapshot.safeString(camelCaseKey: String, defaultValue: String = ""): String {
    val snake = camelCaseKey.camelToSnake()
    return getString(camelCaseKey) ?: getString(snake) ?: get(camelCaseKey)?.toString() ?: get(snake)?.toString() ?: defaultValue
}

private fun DocumentSnapshot.safeLong(camelCaseKey: String, defaultValue: Long = 0L): Long {
    val snake = camelCaseKey.camelToSnake()
    getLong(camelCaseKey)?.let { return it }
    getLong(snake)?.let { return it }
    val v1 = get(camelCaseKey)
    if (v1 is Number) return v1.toLong()
    val v2 = get(snake)
    if (v2 is Number) return v2.toLong()
    return getString(camelCaseKey)?.toLongOrNull() ?: getString(snake)?.toLongOrNull() ?: defaultValue
}

private fun DocumentSnapshot.safeLongOrNull(camelCaseKey: String): Long? {
    val snake = camelCaseKey.camelToSnake()
    getLong(camelCaseKey)?.let { return it }
    getLong(snake)?.let { return it }
    val v1 = get(camelCaseKey)
    if (v1 is Number) return v1.toLong()
    val v2 = get(snake)
    if (v2 is Number) return v2.toLong()
    return getString(camelCaseKey)?.toLongOrNull() ?: getString(snake)?.toLongOrNull()
}

private fun DocumentSnapshot.safeInt(camelCaseKey: String, defaultValue: Int = 0): Int {
    return safeLong(camelCaseKey, defaultValue.toLong()).toInt()
}

private fun DocumentSnapshot.safeIntOrNull(camelCaseKey: String): Int? {
    return safeLongOrNull(camelCaseKey)?.toInt()
}

private fun DocumentSnapshot.safeDouble(camelCaseKey: String, defaultValue: Double = 0.0): Double {
    val snake = camelCaseKey.camelToSnake()
    getDouble(camelCaseKey)?.let { return it }
    getDouble(snake)?.let { return it }
    getLong(camelCaseKey)?.let { return it.toDouble() }
    getLong(snake)?.let { return it.toDouble() }
    val v1 = get(camelCaseKey)
    if (v1 is Number) return v1.toDouble()
    val v2 = get(snake)
    if (v2 is Number) return v2.toLong().toDouble()
    return getString(camelCaseKey)?.toDoubleOrNull() ?: getString(snake)?.toDoubleOrNull() ?: defaultValue
}

private fun DocumentSnapshot.safeStringOrNull(camelCaseKey: String): String? {
    val snake = camelCaseKey.camelToSnake()
    return getString(camelCaseKey) ?: getString(snake) ?: get(camelCaseKey)?.toString() ?: get(snake)?.toString()
}

private fun DocumentSnapshot.safeBoolean(camelCaseKey: String, defaultValue: Boolean = false): Boolean {
    val snake = camelCaseKey.camelToSnake()
    getBoolean(camelCaseKey)?.let { return it }
    getBoolean(snake)?.let { return it }
    val v1 = get(camelCaseKey)
    if (v1 is Boolean) return v1
    val v2 = get(snake)
    if (v2 is Boolean) return v2
    return getString(camelCaseKey)?.toBoolean() ?: getString(snake)?.toBoolean() ?: defaultValue
}

private fun DocumentSnapshot.safeDate(camelCaseKey: String, defaultDate: Date = Date()): Date {
    val snake = camelCaseKey.camelToSnake()
    val raw = try {
        get(camelCaseKey) ?: get(snake) ?: return defaultDate
    } catch (e: Exception) {
        return defaultDate
    }
    
    return when (raw) {
        is Timestamp -> raw.toDate()
        is Date -> raw
        is Number -> Date(raw.toLong())
        is String -> {
            if (raw.isBlank()) defaultDate
            else {
                try {
                    isoFormat.parse(raw) ?: raw.toLongOrNull()?.let { Date(it) } ?: defaultDate
                } catch (e: Exception) {
                    raw.toLongOrNull()?.let { Date(it) } ?: defaultDate
                }
            }
        }
        else -> {
            try {
                getDate(camelCaseKey) ?: getDate(snake) ?: defaultDate
            } catch (e: Exception) {
                defaultDate
            }
        }
    }
}

private fun DocumentSnapshot.safeStringList(camelCaseKey: String): List<String> {
    val snake = camelCaseKey.camelToSnake()
    val obj1 = get(camelCaseKey)
    if (obj1 is List<*>) {
        return obj1.mapNotNull { it?.toString() }
    }
    val obj2 = get(snake)
    if (obj2 is List<*>) {
        return obj2.mapNotNull { it?.toString() }
    }
    return emptyList()
}