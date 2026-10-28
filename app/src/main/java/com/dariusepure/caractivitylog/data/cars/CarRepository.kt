package com.dariusepure.caractivitylog.data.cars

import android.util.Log
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.domain.Car
// import com.dariusepure.caractivitylog.domain.CarReport
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
import com.dariusepure.caractivitylog.util.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val localStorageHelper: LocalStorageHelper,
    private val networkMonitor: NetworkMonitor
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
    // private val reportsCache = MutableStateFlow<Map<String, List<CarReport>>>(emptyMap())
    private val diagnosisCache = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    private var carsListenerRegistration: ListenerRegistration? = null

    init {
        repositoryScope.launch {
            authRepository.userId.collect { uid ->
                setupUserSession(uid ?: "guest")
            }
        }
    }

    private fun isCloudUser(): Boolean {
        val uid = authRepository.getUserId() ?: "guest"
        return uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID
    }

    private fun getUidSafe(): String {
        return authRepository.getUserId() ?: "guest"
    }

    private suspend fun setupUserSession(uid: String) {
        carsListenerRegistration?.remove()
        carsListenerRegistration = null

        // Clear all memory caches immediately to prevent cross-account data leaks
        carsCache.value = emptyList()
        inspectionsCache.value = emptyMap()
        fuelLogsCache.value = emptyMap()
        maintenanceLogsCache.value = emptyMap()
        mileageLogsCache.value = emptyMap()
        insurancesCache.value = emptyMap()
        vignettesCache.value = emptyMap()
        tireSetsCache.value = emptyMap()
        diagnosisCache.value = emptyMap()

        if (uid == "guest" || uid == AuthRepository.GUEST_UID) {
            withContext(Dispatchers.IO) {
                try {
                    localStorageHelper.clearAllLocalData()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error clearing local cache: ${e.message}")
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    localStorageHelper.clearAllLocalData()
                } catch (e: Exception) {}
            }

            repositoryScope.launch(Dispatchers.IO) {
                try {
                    authRepository.ensureProfileExists(uid, authRepository.currentUserEmail ?: "")
                } catch (e: Exception) {}
            }

            carsListenerRegistration = firestore.collection("users").document(uid).collection("cars")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CarRepository", "Firestore listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteCars = snapshot.documents.mapNotNull { doc ->
                            try { doc.toCar() } catch (e: Exception) { null }
                        }
                        carsCache.value = remoteCars

                        remoteCars.forEach { car ->
                            fetchSubCollectionDirect(uid, car.id)
                        }
                    }
                }
        }
    }

    private fun fetchSubCollectionDirect(uid: String, carId: String) {
        val carDoc = firestore.collection("users").document(uid).collection("cars").document(carId)
        repositoryScope.launch(Dispatchers.IO) {
            try {
                coroutineScope {
                    val insDeferred = async { try { carDoc.collection("inspections").get().await().documents.mapNotNull { it.toVehicleInspection() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val fuelsDeferred = async { try { carDoc.collection("fuel_logs").get().await().documents.mapNotNull { it.toFuelLog() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val maintDeferred = async { try { carDoc.collection("maintenance_logs").get().await().documents.mapNotNull { it.toMaintenance() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val mileageDeferred = async { try { carDoc.collection("mileage_logs").get().await().documents.mapNotNull { it.toMileageLog() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val insurancesDeferred = async { try { carDoc.collection("insurances").get().await().documents.mapNotNull { it.toInsurance() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val vignettesDeferred = async { try { carDoc.collection("vignettes").get().await().documents.mapNotNull { it.toVignette() }.sortedByDescending { it.date } } catch (e: Exception) { emptyList() } }
                    val tiresDeferred = async { try { carDoc.collection("tire_sets").get().await().documents.mapNotNull { it.toTireSet() }.sortedByDescending { it.isActive } } catch (e: Exception) { emptyList() } }
                    val diagDeferred = async { try { carDoc.collection("diagnosis").get().await().documents.mapNotNull { it.toChatMessage() }.sortedBy { it.timestamp } } catch (e: Exception) { emptyList() } }

                    val ins = insDeferred.await()
                    val fuels = fuelsDeferred.await()
                    val maint = maintDeferred.await()
                    val mileage = mileageDeferred.await()
                    val insurances = insurancesDeferred.await()
                    val vignettes = vignettesDeferred.await()
                    val tires = tiresDeferred.await()
                    val diag = diagDeferred.await()

                    inspectionsCache.value = inspectionsCache.value.toMutableMap().apply { put(carId, ins) }
                    fuelLogsCache.value = fuelLogsCache.value.toMutableMap().apply { put(carId, fuels) }
                    maintenanceLogsCache.value = maintenanceLogsCache.value.toMutableMap().apply { put(carId, maint) }
                    mileageLogsCache.value = mileageLogsCache.value.toMutableMap().apply { put(carId, mileage) }
                    insurancesCache.value = insurancesCache.value.toMutableMap().apply { put(carId, insurances) }
                    vignettesCache.value = vignettesCache.value.toMutableMap().apply { put(carId, vignettes) }
                    tireSetsCache.value = tireSetsCache.value.toMutableMap().apply { put(carId, tires) }
                    diagnosisCache.value = diagnosisCache.value.toMutableMap().apply { put(carId, diag) }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching subcollections for car $carId: ${e.message}")
            }
        }
    }

    fun refresh() {
        val uid = getUidSafe()
        if (isCloudUser()) {
            repositoryScope.launch(Dispatchers.IO) {
                setupUserSession(uid)
            }
        }
    }

    val cars: Flow<List<Car>> = carsCache.asStateFlow()

    fun getCarsFromCache(): List<Car> = carsCache.value
    fun getInspectionsFromCache(carId: String): List<VehicleInspection> = inspectionsCache.value[carId] ?: emptyList()
    fun getFuelLogsFromCache(carId: String): List<FuelLog> = fuelLogsCache.value[carId] ?: emptyList()
    fun getMaintenanceLogsFromCache(carId: String): List<Maintenance> = maintenanceLogsCache.value[carId] ?: emptyList()
    fun getMileageLogsFromCache(carId: String): List<MileageLog> = mileageLogsCache.value[carId] ?: emptyList()
    fun getInsurancesFromCache(carId: String): List<Insurance> = insurancesCache.value[carId] ?: emptyList()
    fun getVignettesFromCache(carId: String): List<Vignette> = vignettesCache.value[carId] ?: emptyList()
    fun getTireSetsFromCache(carId: String): List<TireSet> = tireSetsCache.value[carId] ?: emptyList()

    // CAR OPERATIONS
    suspend fun createCar(car: Car) {
        val item = if (car.id.isBlank()) car.copy(id = UUID.randomUUID().toString()) else car
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val remoteCar = item.toRemote().copy(userId = uid)
                firestore.collection("users").document(uid).collection("cars").document(remoteCar.id).set(remoteCar.toFirestoreMap()).await()
            }
        } else {
            val updated = (carsCache.value.filterNot { it.id == item.id } + item)
            carsCache.value = updated
            withContext(Dispatchers.IO) {
                try { localStorageHelper.saveCars(uid, updated) } catch (e: Exception) {}
            }
        }
    }

    suspend fun updateCar(car: Car) {
        createCar(car)
    }

    suspend fun deleteCar(carId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).delete().await()
            }
        } else {
            val updated = carsCache.value.filterNot { it.id == carId }
            carsCache.value = updated
            withContext(Dispatchers.IO) {
                try { localStorageHelper.saveCars(uid, updated) } catch (e: Exception) {}
            }
        }
    }

    suspend fun getCar(carId: String): Car? {
        carsCache.value.find { it.id == carId }?.let { return it }
        val uid = getUidSafe()
        if (isCloudUser()) {
            return try {
                val doc = firestore.collection("users").document(uid).collection("cars").document(carId).get().await()
                doc.toCar()
            } catch (e: Exception) {
                null
            }
        }
        return null
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

    suspend fun isVinDuplicate(vin: String, excludeCarId: String?): Boolean {
        return try {
            carsCache.value.any { it.vin.equals(vin.trim(), ignoreCase = true) && it.id != excludeCarId }
        } catch (e: Exception) {
            false
        }
    }

    // SUBCOLLECTION HELPER
    private fun <T> updateSubCollection(
        cache: MutableStateFlow<Map<String, List<T>>>,
        carId: String,
        saveAction: ((String, Map<String, List<T>>) -> Unit)? = null,
        updateBlock: (List<T>) -> List<T>
    ) {
        val currentMap = cache.value.toMutableMap()
        val currentList = currentMap[carId] ?: emptyList()
        val newList = updateBlock(currentList)
        currentMap[carId] = newList
        cache.value = currentMap

        val uid = getUidSafe()
        if (!isCloudUser() && saveAction != null) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    saveAction(uid, currentMap)
                } catch (e: Exception) {}
            }
        }
    }

    // MILEAGE LOGS
    fun getMileageLogs(carId: String): Flow<List<MileageLog>> {
        if (isCloudUser()) {
            val uid = getUidSafe()
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").get().await()
                    val list = snap.documents.mapNotNull { it.toMileageLog() }.sortedByDescending { it.date }
                    mileageLogsCache.value = mileageLogsCache.value.toMutableMap().apply { put(carId, list) }
                } catch (e: Exception) {}
            }
        }
        return mileageLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = item.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(mileageLogsCache, carId, localStorageHelper::saveMileageLogs) { list ->
                (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
            }
        }
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        addMileageLog(carId, log)
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(logId).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(mileageLogsCache, carId, localStorageHelper::saveMileageLogs) { list ->
                list.filterNot { it.id == logId }
            }
        }
    }

    // INSPECTIONS
    fun getInspections(carId: String): Flow<List<VehicleInspection>> = inspectionsCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addInspection(carId: String, item: VehicleInspection) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(inspectionsCache, carId, localStorageHelper::saveInspections) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.date }
            }
        }
    }
    suspend fun updateInspection(carId: String, item: VehicleInspection) = addInspection(carId, item)
    suspend fun deleteInspection(carId: String, item: VehicleInspection) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(item.id).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(inspectionsCache, carId, localStorageHelper::saveInspections) { list ->
                list.filterNot { it.id == item.id }
            }
        }
    }

    // INSURANCES
    fun getInsurances(carId: String): Flow<List<Insurance>> = insurancesCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addInsurance(carId: String, item: Insurance) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(insurancesCache, carId, localStorageHelper::saveInsurances) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.date }
            }
        }
    }
    suspend fun updateInsurance(carId: String, item: Insurance) = addInsurance(carId, item)
    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(insuranceId).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(insurancesCache, carId, localStorageHelper::saveInsurances) { list ->
                list.filterNot { it.id == insuranceId }
            }
        }
    }

    // VIGNETTES
    fun getVignettes(carId: String): Flow<List<Vignette>> = vignettesCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addVignette(carId: String, item: Vignette) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(vignettesCache, carId, localStorageHelper::saveVignettes) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.date }
            }
        }
    }
    suspend fun updateVignette(carId: String, item: Vignette) = addVignette(carId, item)
    suspend fun deleteVignette(carId: String, vignetteId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(vignetteId).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(vignettesCache, carId, localStorageHelper::saveVignettes) { list ->
                list.filterNot { it.id == vignetteId }
            }
        }
    }

    // TIRE SETS
    fun getTireSets(carId: String): Flow<List<TireSet>> = tireSetsCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addTireSet(carId: String, item: TireSet) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(tireSetsCache, carId, localStorageHelper::saveTireSets) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.isActive }
            }
        }
    }
    suspend fun updateTireSet(carId: String, item: TireSet) = addTireSet(carId, item)
    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(tireSetId).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(tireSetsCache, carId, localStorageHelper::saveTireSets) { list ->
                list.filterNot { it.id == tireSetId }
            }
        }
    }

    // DIAGNOSIS CHAT
    fun getDiagnosisMessages(carId: String): Flow<List<ChatMessage>> = diagnosisCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = RemoteChatMessage.fromChatMessage(message).copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId)
                    .collection("diagnosis").add(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(diagnosisCache, carId, localStorageHelper::saveDiagnosisMessages) { list ->
                list + message
            }
        }
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId)
                    .collection("diagnosis").get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(diagnosisCache, carId, localStorageHelper::saveDiagnosisMessages) { emptyList() }
        }
    }

    // FUEL LOGS
    fun getFuelLogs(carId: String): Flow<List<FuelLog>> = fuelLogsCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addFuelLog(carId: String, item: FuelLog) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(fuelLogsCache, carId, localStorageHelper::saveFuelLogs) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.date }
            }
        }
    }
    suspend fun updateFuelLog(carId: String, item: FuelLog) = addFuelLog(carId, item)
    suspend fun deleteFuelLog(carId: String, item: FuelLog) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(item.id).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(fuelLogsCache, carId, localStorageHelper::saveFuelLogs) { list ->
                list.filterNot { it.id == item.id }
            }
        }
    }

    // MAINTENANCE
    fun getMaintenanceLogs(carId: String): Flow<List<Maintenance>> = maintenanceLogsCache.map { map -> map[carId] ?: emptyList() }
    suspend fun addMaintenanceLog(carId: String, item: Maintenance) {
        val obj = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                val dto = obj.toRemote().copy(carId = carId)
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(dto.id).set(dto.toFirestoreMap()).await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(maintenanceLogsCache, carId, localStorageHelper::saveMaintenanceLogs) { list ->
                (list.filterNot { it.id == obj.id } + obj).sortedByDescending { it.date }
            }
        }
    }
    suspend fun updateMaintenanceLog(carId: String, item: Maintenance) = addMaintenanceLog(carId, item)
    suspend fun deleteMaintenanceLog(carId: String, item: Maintenance) {
        val uid = getUidSafe()
        if (isCloudUser()) {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(item.id).delete().await()
                fetchSubCollectionDirect(uid, carId)
            }
        } else {
            updateSubCollection(maintenanceLogsCache, carId, localStorageHelper::saveMaintenanceLogs) { list ->
                list.filterNot { it.id == item.id }
            }
        }
    }

    // CAR REPORTS REMOVED
}

// ==========================================
// Robust DocumentSnapshot Extension Functions
// ==========================================

fun DocumentSnapshot.toCar(): Car {
    return Car(
        id = id,
        name = safeString("name"),
        licensePlate = safeString("licensePlate", safeString("license_plate")),
        plateCountry = safeString("plateCountry", safeString("plate_country", "RO")),
        make = safeString("make"),
        model = safeString("model"),
        vin = safeString("vin"),
        year = safeInt("year"),
        engineSize = safeString("engineSize", safeString("engine_size")),
        fuelType = safeString("fuelType", safeString("fuel_type")),
        fuelSystem = safeString("fuelSystem", safeString("fuel_system")),
        color = safeString("color"),
        power = safeInt("power"),
        powerUnit = safeString("powerUnit", safeString("power_unit", "hp")),
        torque = safeInt("torque"),
        engineCode = safeString("engineCode", safeString("engine_code")),
        engineLayout = safeString("engineLayout", safeString("engine_layout")),
        cylinderLayout = safeString("cylinderLayout", safeString("cylinder_layout")),
        length = safeInt("length"),
        width = safeInt("width"),
        height = safeInt("height"),
        wheelbase = safeInt("wheelbase"),
        emissionStandard = safeString("emissionStandard", safeString("emission_standard")),
        aspiration = safeString("aspiration"),
        fuelTankCapacity = safeDouble("fuelTankCapacity", safeDouble("fuel_tank_capacity")),
        batteryCapacity = safeDouble("batteryCapacity", safeDouble("battery_capacity")),
        drivetrain = safeString("drivetrain"),
        gearboxType = safeString("gearboxType", safeString("gearbox_type")),
        gears = safeString("gears"),
        frontSuspension = safeString("frontSuspension", safeString("front_suspension")),
        rearSuspension = safeString("rearSuspension", safeString("rear_suspension")),
        frontBrakes = safeString("frontBrakes", safeString("front_brakes")),
        rearBrakes = safeString("rearBrakes", safeString("rear_brakes")),
        vehicleType = safeString("vehicleType", safeString("vehicle_type")),
        manufacturingCountry = safeString("manufacturingCountry", safeString("manufacturing_country")),
        topSpeed = safeDouble("topSpeed", safeDouble("top_speed")),
        acceleration0to100 = safeDouble("acceleration0to100", safeDouble("acceleration_0_to_100")),
        fuelConsumptionCombined = safeDouble("fuelConsumptionCombined", safeDouble("fuel_consumption_combined")),
        fuelConsumptionUrban = safeDouble("fuelConsumptionUrban", safeDouble("fuel_consumption_urban")),
        fuelConsumptionExtraUrban = safeDouble("fuelConsumptionExtraUrban", safeDouble("fuel_consumption_extra_urban")),
        co2Emissions = safeInt("co2Emissions", safeInt("co2_emissions")),
        weight = safeInt("weight"),
        numberOfSeats = safeInt("numberOfSeats", safeInt("number_of_seats")),
        numberOfCylinders = safeInt("numberOfCylinders", safeInt("number_of_cylinders")),
        valvesPerCylinder = safeInt("valvesPerCylinder", safeInt("valves_per_cylinder")),
        numberOfDoors = safeInt("numberOfDoors", safeInt("number_of_doors")),
        bootSpace = safeInt("bootSpace", safeInt("boot_space")),
        tireWidth = safeInt("tireWidth", safeInt("tire_width")),
        tireAspectRatio = safeInt("tireAspectRatio", safeInt("tire_aspect_ratio")),
        tireDiameter = safeInt("tireDiameter", safeInt("tire_diameter")),
        equipments = safeStringList("equipments"),
        accentColor = safeLongOrNull("accentColor") ?: safeLongOrNull("accent_color"),
        createdAt = safeDate("createdAt", safeDate("created_at", Date())),
        updatedAt = safeDate("updatedAt", safeDate("updated_at", Date())),
        activityCount = safeInt("activityCount", safeInt("activity_count")),
        airbags = safeInt("airbags"),
        generation = safeString("generation"),
        engineVariant = safeString("engineVariant", safeString("engine_variant")),
        isPendingSync = false
    )
}

fun DocumentSnapshot.toMileageLog(): MileageLog {
    val value = safeDouble("mileage", safeDouble("km", 0.0))
    return MileageLog(
        id = id,
        km = value,
        date = safeDate("date", Date())
    )
}

fun DocumentSnapshot.toVehicleInspection(): VehicleInspection {
    val unitStr = safeString("durationUnit", safeString("duration_unit", "YEARS"))
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.YEARS
    }
    return VehicleInspection(
        id = id,
        date = safeDate("date", Date()),
        mileage = safeDouble("mileage", safeDouble("km", 0.0)),
        durationValue = safeInt("durationValue", safeInt("duration_value", 1)),
        durationUnit = durationUnitEnum,
        mileageLogId = safeString("mileageLogId", safeString("mileage_log_id"))
    )
}

fun DocumentSnapshot.toInsurance(): Insurance {
    val unitStr = safeString("durationUnit", safeString("duration_unit", "MONTHS"))
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.MONTHS
    }
    return Insurance(
        id = id,
        date = safeDate("date", Date()),
        durationValue = safeInt("durationValue", safeInt("duration_value", 6)),
        durationUnit = durationUnitEnum,
        provider = safeString("provider")
    )
}

fun DocumentSnapshot.toVignette(): Vignette {
    val unitStr = safeString("durationUnit", safeString("duration_unit", "MONTHS"))
    val durationUnitEnum = try {
        InspectionDurationUnit.valueOf(unitStr)
    } catch (e: Exception) {
        InspectionDurationUnit.MONTHS
    }
    return Vignette(
        id = id,
        date = safeDate("date", Date()),
        durationValue = safeInt("durationValue", safeInt("duration_value", 1)),
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
        isActive = safeBoolean("isActive", safeBoolean("is_active", false))
    )
}

fun DocumentSnapshot.toFuelLog(): FuelLog {
    return FuelLog(
        id = id,
        date = safeDate("date", Date()),
        km = safeDouble("km", safeDouble("mileage", 0.0)),
        liters = safeDouble("liters"),
        isFullTank = safeBoolean("isFullTank", safeBoolean("is_full_tank", true)),
        mileageLogId = safeString("mileageLogId", safeString("mileage_log_id"))
    )
}

fun DocumentSnapshot.toMaintenance(): Maintenance {
    return Maintenance(
        id = id,
        date = safeDate("date", Date()),
        km = safeDouble("km", safeDouble("mileage", 0.0)),
        description = safeString("description"),
        mileageLogId = safeString("mileageLogId", safeString("mileage_log_id")),
        category = safeString("category", "General")
    )
}

    // toCarReport removed

fun DocumentSnapshot.toChatMessage(): ChatMessage {
    return ChatMessage(
        text = safeString("text"),
        isUser = safeBoolean("isUser", safeBoolean("is_user", false)),
        timestamp = safeLong("timestamp", System.currentTimeMillis())
    )
}

// ==========================================
// Safe Field Reading Helpers
// ==========================================

private fun String.camelToSnake(): String {
    return this
        .replace("0to100", "_0_to_100")
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .lowercase()
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
    val raw = try { get(camelCaseKey) ?: get(snake) } catch (e: Exception) { null }
    return when (raw) {
        is Number -> raw.toLong()
        is Timestamp -> raw.toDate().time
        is Date -> raw.time
        is String -> raw.toLongOrNull() ?: defaultValue
        else -> defaultValue
    }
}

private fun DocumentSnapshot.safeLongOrNull(camelCaseKey: String): Long? {
    val snake = camelCaseKey.camelToSnake()
    val raw = try { get(camelCaseKey) ?: get(snake) } catch (e: Exception) { null }
    return when (raw) {
        is Number -> raw.toLong()
        is Timestamp -> raw.toDate().time
        is Date -> raw.time
        is String -> raw.toLongOrNull()
        else -> null
    }
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
