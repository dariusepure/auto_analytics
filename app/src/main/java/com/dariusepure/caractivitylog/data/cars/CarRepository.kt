package com.dariusepure.caractivitylog.data.cars

import android.util.Log
import com.dariusepure.caractivitylog.data.auth.AuthEvent
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.local.CsvToRoomMigrator
import com.dariusepure.caractivitylog.data.local.dao.CarDao
import com.dariusepure.caractivitylog.data.local.dao.FuelLogDao
import com.dariusepure.caractivitylog.data.local.dao.InsuranceDao
import com.dariusepure.caractivitylog.data.local.dao.MaintenanceDao
import com.dariusepure.caractivitylog.data.local.dao.MileageLogDao
import com.dariusepure.caractivitylog.data.local.dao.TireSetDao
import com.dariusepure.caractivitylog.data.local.dao.VehicleInspectionDao
import com.dariusepure.caractivitylog.data.local.dao.VignetteDao
import com.dariusepure.caractivitylog.data.local.entities.toDomain
import com.dariusepure.caractivitylog.data.local.entities.toEntity
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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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
    private val carDao: CarDao,
    private val vehicleInspectionDao: VehicleInspectionDao,
    private val fuelLogDao: FuelLogDao,
    private val maintenanceDao: MaintenanceDao,
    private val mileageLogDao: MileageLogDao,
    private val insuranceDao: InsuranceDao,
    private val vignetteDao: VignetteDao,
    private val tireSetDao: TireSetDao,
    private val csvToRoomMigrator: CsvToRoomMigrator
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

    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    init {
        runBlocking(Dispatchers.IO) {
            try {
                csvToRoomMigrator.migrateIfNeeded()
                val initialUid = authRepository.getUserId() ?: "guest"
                val allLocalCars = carDao.getCarsForUser(initialUid).map { it.toDomain() }
                if (allLocalCars.isNotEmpty()) {
                    carsCache.value = allLocalCars
                    val carIds = allLocalCars.map { it.id }
                    inspectionsCache.value = vehicleInspectionDao.getInspectionsForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    fuelLogsCache.value = fuelLogDao.getFuelLogsForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    maintenanceLogsCache.value = maintenanceDao.getMaintenanceLogsForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    mileageLogsCache.value = mileageLogDao.getMileageLogsForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    insurancesCache.value = insuranceDao.getInsurancesForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    vignettesCache.value = vignetteDao.getVignettesForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                    tireSetsCache.value = tireSetDao.getTireSetsForCars(carIds).groupBy { it.carId }.mapValues { entry -> entry.value.map { it.toDomain() } }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error in sync init cache: ${e.message}")
            }
        }

        repositoryScope.launch {
            authRepository.userId.collect { uid ->
                val activeUid = uid ?: "guest"
                migrateGuestDataIfAny(activeUid)
                switchUserCache(activeUid)
                if (uid != null && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    syncAllAccountDataToLocalRoom(uid)
                }
            }
        }

        repositoryScope.launch {
            authRepository.authEvents.collect { event ->
                if (event is AuthEvent.SyncCompleted) {
                    val activeUid = getUidSafe()
                    if (activeUid != "guest" && activeUid != AuthRepository.GUEST_UID) {
                        syncAllAccountDataToLocalRoom(activeUid)
                    }
                }
            }
        }
    }

    private suspend fun migrateGuestDataIfAny(targetUid: String) {
        if (targetUid == "guest" || targetUid == AuthRepository.GUEST_UID || targetUid.isBlank()) return

        val guestUids = listOf("guest", "local_guest_user", AuthRepository.GUEST_UID, "", "unknown_uid")
        guestUids.forEach { guestUid ->
            if (guestUid == targetUid) return@forEach
            val guestCars = carDao.getCarsForUser(guestUid).map { it.toDomain() }
            if (guestCars.isNotEmpty()) {
                Log.d("CarRepository", "Migrating ${guestCars.size} guest cars from '$guestUid' to targetUid: $targetUid")
                val currentCars = carDao.getCarsForUser(targetUid).map { it.toDomain() }.toMutableList()
                val existingIds = currentCars.map { it.id }.toSet()

                guestCars.forEach { gCar ->
                    if (!existingIds.contains(gCar.id)) {
                        currentCars.add(gCar)
                        repositoryScope.launch {
                            try {
                                val email = authRepository.currentUserEmail ?: ""
                                authRepository.ensureProfileExists(targetUid, email)
                                val remoteCar = gCar.toRemote().copy(userId = targetUid)
                                firestore.collection("users").document(targetUid).collection("cars").document(remoteCar.id).set(remoteCar).await()
                            } catch (e: Exception) {
                                Log.e("CarRepository", "Error migrating guest car ${gCar.id}: ${e.message}")
                            }
                        }
                    }
                }

                carDao.updateUserId(guestUid, targetUid)
                carsCache.value = currentCars
            }
        }
    }

    fun refresh() {
        val uid = getUidSafe()
        if (uid != "guest" && uid != AuthRepository.GUEST_UID) {
            syncAllAccountDataToLocalRoom(uid)
        }
    }

    private fun getUidSafe(): String {
        return authRepository.getUserId() ?: "guest"
    }

    private suspend fun switchUserCache(uid: String) {
        var cars = carDao.getCarsForUser(uid).map { it.toDomain() }
        if (cars.isEmpty() && uid != "guest" && uid != AuthRepository.GUEST_UID && uid.isNotBlank()) {
            migrateGuestDataIfAny(uid)
            cars = carDao.getCarsForUser(uid).map { it.toDomain() }
        }
        carsCache.value = cars

        val carIds = cars.map { it.id }
        if (carIds.isNotEmpty()) {
            inspectionsCache.value = vehicleInspectionDao.getInspectionsForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            fuelLogsCache.value = fuelLogDao.getFuelLogsForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            maintenanceLogsCache.value = maintenanceDao.getMaintenanceLogsForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            mileageLogsCache.value = mileageLogDao.getMileageLogsForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            insurancesCache.value = insuranceDao.getInsurancesForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            vignettesCache.value = vignetteDao.getVignettesForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }

            tireSetsCache.value = tireSetDao.getTireSetsForCars(carIds)
                .groupBy { it.carId }
                .mapValues { entry -> entry.value.map { it.toDomain() } }
        } else {
            inspectionsCache.value = emptyMap()
            fuelLogsCache.value = emptyMap()
            maintenanceLogsCache.value = emptyMap()
            mileageLogsCache.value = emptyMap()
            insurancesCache.value = emptyMap()
            vignettesCache.value = emptyMap()
            tireSetsCache.value = emptyMap()
        }
    }

    // Cache Update Helpers
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
        repositoryScope.launch {
            carDao.insertCars(updated.map { it.toEntity(uid) })
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

    // CARS
    val cars: Flow<List<Car>> = carsCache.asStateFlow()

    private fun syncAllAccountDataToLocalRoom(uid: String) {
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val email = authRepository.currentUserEmail
                if (uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    authRepository.ensureProfileExists(uid, email ?: "")
                }

                val allRemoteCars = mutableListOf<Car>()
                val carDocRefs = mutableMapOf<String, DocumentReference>()

                // 1. Fetch from users/{uid}/cars
                if (uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    try {
                        val userCarsSnap = firestore.collection("users").document(uid).collection("cars").get().await()
                        for (doc in userCarsSnap.documents) {
                            val car = doc.toCar()
                            if (allRemoteCars.none { it.id == car.id }) {
                                allRemoteCars.add(car)
                                carDocRefs[car.id] = doc.reference
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CarRepository", "Error fetching user cars for $uid: ${e.message}")
                    }
                }



                val remoteCars = allRemoteCars
                val localCars = carDao.getCarsForUser(uid).map { it.toDomain() }
                val localMap = localCars.associateBy { it.id }
                val remoteMap = remoteCars.associateBy { it.id }

                val mergedCars = remoteCars.map { remote ->
                    val local = localMap[remote.id]
                    if (local != null) {
                        remote.copy(
                            vin = if (remote.vin.isBlank()) local.vin else remote.vin,
                            licensePlate = if (remote.licensePlate.isBlank()) local.licensePlate else remote.licensePlate,
                            engineVariant = if (remote.engineVariant.isBlank()) local.engineVariant else remote.engineVariant,
                            generation = if (remote.generation.isBlank()) local.generation else remote.generation
                        )
                    } else {
                        remote
                    }
                }.toMutableList()

                localCars.forEach { local ->
                    if (!remoteMap.containsKey(local.id)) {
                        if (local.isPendingSync) {
                            mergedCars.add(local)
                            repositoryScope.launch {
                                try {
                                    authRepository.ensureProfileExists(uid, email ?: "")
                                    val remoteCar = local.toRemote().copy(userId = uid)
                                    val targetRef = firestore.collection("users").document(uid).collection("cars").document(remoteCar.id)
                                    targetRef.set(remoteCar).await()
                                    carDocRefs[remoteCar.id] = targetRef
                                    Log.d("CarRepository", "Synced local unsynced car ${local.id} to remote")
                                } catch (e: Exception) {
                                    Log.e("CarRepository", "Error pushing local car ${local.id} to remote: ${e.message}")
                                }
                            }
                        } else {
                            repositoryScope.launch {
                                try {
                                    carDao.deleteCar(local.id)
                                    Log.d("CarRepository", "Removed leftover/foreign car ${local.id} from local DB")
                                } catch (e: Exception) {
                                    Log.e("CarRepository", "Error deleting leftover car ${local.id}: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val remote = remoteMap[local.id]
                        if (remote != null && remote.vin.isBlank() && local.vin.isNotBlank()) {
                            repositoryScope.launch {
                                try {
                                    val remoteCar = local.toRemote().copy(userId = uid)
                                    val targetRef = carDocRefs[local.id] ?: firestore.collection("users").document(uid).collection("cars").document(remoteCar.id)
                                    targetRef.set(remoteCar).await()
                                    Log.d("CarRepository", "Pushed updated VIN for car ${local.id} to remote")
                                } catch (e: Exception) {
                                    Log.e("CarRepository", "Error pushing VIN to remote: ${e.message}")
                                }
                            }
                        }
                    }
                }

                carsCache.value = mergedCars
                carDao.insertCars(mergedCars.map { it.toEntity(uid) })

                // 2. Fetch history records for every car using its resolved DocumentReference
                remoteCars.forEach { car ->
                    val carId = car.id
                    val carDocRef = carDocRefs[carId] ?: firestore.collection("users").document(uid).collection("cars").document(carId)

                    // Inspections
                    try {
                        val insSnap = carDocRef.collection("inspections").get().await()
                        val ins = insSnap.documents.mapNotNull { doc ->
                            doc.toVehicleInspection()
                        }.sortedByDescending { it.date }
                        if (ins.isNotEmpty()) {
                            updateMapCache(inspectionsCache, carId) { ins }
                            vehicleInspectionDao.insertInspections(ins.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync inspections error for $carId: ${e.message}")
                    }
                    try {
                        val fuelSnap = carDocRef.collection("fuel_logs").get().await()
                        val fuels = fuelSnap.documents.mapNotNull { doc ->
                            doc.toFuelLog()
                        }.sortedByDescending { it.date }
                        if (fuels.isNotEmpty()) {
                            updateMapCache(fuelLogsCache, carId) { fuels }
                            fuelLogDao.insertFuelLogs(fuels.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync fuel_logs error for $carId: ${e.message}")
                    }

                    // Maintenance logs
                    try {
                        val maintSnap = carDocRef.collection("maintenance_logs").get().await()
                        val maint = maintSnap.documents.mapNotNull { doc ->
                            doc.toMaintenance()
                        }.sortedByDescending { it.date }
                        if (maint.isNotEmpty()) {
                            updateMapCache(maintenanceLogsCache, carId) { maint }
                            maintenanceDao.insertMaintenanceLogs(maint.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync maintenance_logs error for $carId: ${e.message}")
                    }

                    // Mileage logs
                    try {
                        val mileageSnap = carDocRef.collection("mileage_logs").get().await()
                        val mileage = mileageSnap.documents.mapNotNull { doc ->
                            doc.toMileageLog()
                        }.sortedByDescending { it.date }
                        if (mileage.isNotEmpty()) {
                            updateMapCache(mileageLogsCache, carId) { mileage }
                            mileageLogDao.insertMileageLogs(mileage.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync mileage_logs error for $carId: ${e.message}")
                    }

                    // Insurances
                    try {
                        val insuSnap = carDocRef.collection("insurances").get().await()
                        val insu = insuSnap.documents.mapNotNull { doc ->
                            doc.toInsurance()
                        }.sortedByDescending { it.date }
                        if (insu.isNotEmpty()) {
                            updateMapCache(insurancesCache, carId) { insu }
                            insuranceDao.insertInsurances(insu.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync insurances error for $carId: ${e.message}")
                    }

                    // Vignettes
                    try {
                        val vigSnap = carDocRef.collection("vignettes").get().await()
                        val vig = vigSnap.documents.mapNotNull { doc ->
                            doc.toVignette()
                        }.sortedByDescending { it.date }
                        if (vig.isNotEmpty()) {
                            updateMapCache(vignettesCache, carId) { vig }
                            vignetteDao.insertVignettes(vig.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync vignettes error for $carId: ${e.message}")
                    }

                    // Tire sets
                    try {
                        val tiresSnap = carDocRef.collection("tire_sets").get().await()
                        val tires = tiresSnap.documents.mapNotNull { doc ->
                            doc.toTireSet()
                        }.sortedByDescending { it.isActive }
                        if (tires.isNotEmpty()) {
                            updateMapCache(tireSetsCache, carId) { tires }
                            tireSetDao.insertTireSets(tires.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync tire_sets error for $carId: ${e.message}")
                    }
                }

                Log.d("CarRepository", "Full user Room sync completed for UID: $uid")
            } catch (e: Exception) {
                Log.e("CarRepository", "Error in syncAllAccountDataToLocalRoom: ${e.message}")
            }
        }
    }

    suspend fun createCar(car: Car) {
        val item = if (car.id.isBlank()) car.copy(id = UUID.randomUUID().toString()) else car
        val uid = getUidSafe()

        updateCarsCache { list -> (list.filterNot { it.id == item.id } + item) }

        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    val email = authRepository.currentUserEmail ?: ""
                    authRepository.ensureProfileExists(uid, email)

                    val remoteCar = item.toRemote().copy(userId = uid)
                    firestore.collection("users").document(uid).collection("cars").document(remoteCar.id).set(remoteCar).await()
                    Log.d("CarRepository", "createCar synced successfully for $uid")
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing createCar: ${e.message}", e)
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

        val local = try { carDao.getCarById(carId)?.toDomain() } catch (e: Exception) { null }
        if (local != null) return local

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
        repositoryScope.launch {
            try {
                carDao.deleteCar(carId)
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
        if (mileageLogsCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = mileageLogDao.getMileageLogsForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(mileageLogsCache, carId) { local }
                }
            }
        }
        fetchMileageLogsFromRemote(carId)
        return mileageLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchMileageLogsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toMileageLog()
                }.sortedByDescending { it.date }
                updateMapCache(mileageLogsCache, carId) { results }
                mileageLogDao.insertMileageLogs(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching mileage_logs: ${e.message}")
            }
        }
    }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                mileageLogDao.insertMileageLogs(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMileageLog: ${e.message}")
            }
        }
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                mileageLogDao.insertMileageLogs(listOf(log.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = log.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMileageLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        updateMapCache(mileageLogsCache, carId) { list -> list.filterNot { it.id == logId } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                mileageLogDao.deleteMileageLog(logId)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("mileage_logs").document(logId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteMileageLog: ${e.message}")
            }
        }
    }

    // INSPECTIONS (ITP)
    fun getInspections(carId: String): Flow<List<VehicleInspection>> {
        if (inspectionsCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = vehicleInspectionDao.getInspectionsForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(inspectionsCache, carId) { local }
                }
            }
        }
        fetchInspectionsFromRemote(carId)
        return inspectionsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchInspectionsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toVehicleInspection()
                }.sortedByDescending { it.date }
                updateMapCache(inspectionsCache, carId) { results }
                vehicleInspectionDao.insertInspections(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching inspections: ${e.message}")
            }
        }
    }

    suspend fun addInspection(carId: String, inspection: VehicleInspection) {
        val item = if (inspection.id.isBlank()) inspection.copy(id = UUID.randomUUID().toString()) else inspection
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vehicleInspectionDao.insertInspections(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInspection: ${e.message}")
            }
        }
    }

    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == inspection.id } + inspection).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vehicleInspectionDao.insertInspections(listOf(inspection.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = inspection.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInspection: ${e.message}")
            }
        }
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        updateMapCache(inspectionsCache, carId) { list -> list.filterNot { it.id == inspection.id } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vehicleInspectionDao.deleteInspection(inspection.id)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("inspections").document(inspection.id).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteInspection: ${e.message}")
            }
        }
    }

    // INSURANCES (RCA/CASCO)
    fun getInsurances(carId: String): Flow<List<Insurance>> {
        if (insurancesCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = insuranceDao.getInsurancesForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(insurancesCache, carId) { local }
                }
            }
        }
        fetchInsurancesFromRemote(carId)
        return insurancesCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchInsurancesFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toInsurance()
                }.sortedByDescending { it.date }
                updateMapCache(insurancesCache, carId) { results }
                insuranceDao.insertInsurances(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching insurances: ${e.message}")
            }
        }
    }

    suspend fun addInsurance(carId: String, insurance: Insurance) {
        val item = if (insurance.id.isBlank()) insurance.copy(id = UUID.randomUUID().toString()) else insurance
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                insuranceDao.insertInsurances(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInsurance: ${e.message}")
            }
        }
    }

    suspend fun updateInsurance(carId: String, insurance: Insurance) {
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == insurance.id } + insurance).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                insuranceDao.insertInsurances(listOf(insurance.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = insurance.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInsurance: ${e.message}")
            }
        }
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        updateMapCache(insurancesCache, carId) { list -> list.filterNot { it.id == insuranceId } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                insuranceDao.deleteInsurance(insuranceId)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("insurances").document(insuranceId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteInsurance: ${e.message}")
            }
        }
    }

    // VIGNETTES
    fun getVignettes(carId: String): Flow<List<Vignette>> {
        if (vignettesCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = vignetteDao.getVignettesForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(vignettesCache, carId) { local }
                }
            }
        }
        fetchVignettesFromRemote(carId)
        return vignettesCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchVignettesFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toVignette()
                }.sortedByDescending { it.date }
                updateMapCache(vignettesCache, carId) { results }
                vignetteDao.insertVignettes(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching vignettes: ${e.message}")
            }
        }
    }

    suspend fun addVignette(carId: String, vignette: Vignette) {
        val item = if (vignette.id.isBlank()) vignette.copy(id = UUID.randomUUID().toString()) else vignette
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vignetteDao.insertVignettes(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addVignette: ${e.message}")
            }
        }
    }

    suspend fun updateVignette(carId: String, vignette: Vignette) {
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == vignette.id } + vignette).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vignetteDao.insertVignettes(listOf(vignette.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = vignette.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateVignette: ${e.message}")
            }
        }
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        updateMapCache(vignettesCache, carId) { list -> list.filterNot { it.id == vignetteId } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                vignetteDao.deleteVignette(vignetteId)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("vignettes").document(vignetteId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteVignette: ${e.message}")
            }
        }
    }

    // TIRE SETS
    fun getTireSets(carId: String): Flow<List<TireSet>> {
        if (tireSetsCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = tireSetDao.getTireSetsForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(tireSetsCache, carId) { local }
                }
            }
        }
        fetchTireSetsFromRemote(carId)
        return tireSetsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchTireSetsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toTireSet()
                }.sortedByDescending { it.isActive }
                updateMapCache(tireSetsCache, carId) { results }
                tireSetDao.insertTireSets(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching tire_sets: ${e.message}")
            }
        }
    }

    suspend fun addTireSet(carId: String, tireSet: TireSet) {
        val item = if (tireSet.id.isBlank()) tireSet.copy(id = UUID.randomUUID().toString()) else tireSet
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.isActive }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                tireSetDao.insertTireSets(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addTireSet: ${e.message}")
            }
        }
    }

    suspend fun updateTireSet(carId: String, tireSet: TireSet) {
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == tireSet.id } + tireSet).sortedByDescending { it.isActive }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                tireSetDao.insertTireSets(listOf(tireSet.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = tireSet.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateTireSet: ${e.message}")
            }
        }
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        updateMapCache(tireSetsCache, carId) { list -> list.filterNot { it.id == tireSetId } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                tireSetDao.deleteTireSet(tireSetId)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("tire_sets").document(tireSetId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteTireSet: ${e.message}")
            }
        }
    }

    // DIAGNOSIS CHAT
    fun getDiagnosisMessages(carId: String): Flow<List<ChatMessage>> {
        fetchDiagnosisMessagesFromRemote(carId)
        return diagnosisCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchDiagnosisMessagesFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("diagnosis_logs").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toChatMessage()
                }.sortedBy { it.timestamp }
                updateMapCache(diagnosisCache, carId) { results }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching diagnosis_logs: ${e.message}")
            }
        }
    }

    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        updateMapCache(diagnosisCache, carId) { list -> list + message }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    val dto = RemoteChatMessage.fromChatMessage(message).copy(carId = carId)
                    val msgId = "${message.timestamp}_${UUID.randomUUID().toString().substring(0, 6)}"
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("diagnosis_logs").document(msgId).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addDiagnosisMessage: ${e.message}")
                }
            }
        }
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        updateMapCache(diagnosisCache, carId) { emptyList() }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("diagnosis_logs").get().await()
                    val batch = firestore.batch()
                    snap.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error clearing diagnosis_logs: ${e.message}")
                }
            }
        }
    }

    // FUEL LOGS
    fun getFuelLogs(carId: String): Flow<List<FuelLog>> {
        if (fuelLogsCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = fuelLogDao.getFuelLogsForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(fuelLogsCache, carId) { local }
                }
            }
        }
        fetchFuelLogsFromRemote(carId)
        return fuelLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchFuelLogsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toFuelLog()
                }.sortedByDescending { it.date }
                updateMapCache(fuelLogsCache, carId) { results }
                fuelLogDao.insertFuelLogs(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching fuel_logs: ${e.message}")
            }
        }
    }

    suspend fun addFuelLog(carId: String, log: FuelLog) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                fuelLogDao.insertFuelLogs(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addFuelLog: ${e.message}")
            }
        }
    }

    suspend fun updateFuelLog(carId: String, log: FuelLog) {
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                fuelLogDao.insertFuelLogs(listOf(log.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = log.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateFuelLog: ${e.message}")
            }
        }
    }

    suspend fun deleteFuelLog(carId: String, log: FuelLog) {
        updateMapCache(fuelLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                fuelLogDao.deleteFuelLog(log.id)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("fuel_logs").document(log.id).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteFuelLog: ${e.message}")
            }
        }
    }

    // MAINTENANCE LOGS
    fun getMaintenanceLogs(carId: String): Flow<List<Maintenance>> {
        if (maintenanceLogsCache.value[carId].isNullOrEmpty()) {
            repositoryScope.launch {
                val local = maintenanceDao.getMaintenanceLogsForCar(carId).map { it.toDomain() }
                if (local.isNotEmpty()) {
                    updateMapCache(maintenanceLogsCache, carId) { local }
                }
            }
        }
        fetchMaintenanceLogsFromRemote(carId)
        return maintenanceLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchMaintenanceLogsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toMaintenance()
                }.sortedByDescending { it.date }
                updateMapCache(maintenanceLogsCache, carId) { results }
                maintenanceDao.insertMaintenanceLogs(results.map { it.toEntity(carId) })
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching maintenance_logs: ${e.message}")
            }
        }
    }

    suspend fun addMaintenanceLog(carId: String, log: Maintenance) {
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                maintenanceDao.insertMaintenanceLogs(listOf(item.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun updateMaintenanceLog(carId: String, log: Maintenance) {
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                maintenanceDao.insertMaintenanceLogs(listOf(log.toEntity(carId)))
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    val dto = log.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(dto.id).set(dto).await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMaintenanceLog(carId: String, log: Maintenance) {
        updateMapCache(maintenanceLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        val uid = getUidSafe()
        repositoryScope.launch {
            try {
                maintenanceDao.deleteMaintenanceLog(log.id)
                if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("maintenance_logs").document(log.id).delete().await()
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteMaintenanceLog: ${e.message}")
            }
        }
    }

    // REPORTS
    fun getCarReports(carId: String): Flow<List<CarReport>> {
        fetchReportsFromRemote(carId)
        return reportsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchReportsFromRemote(carId: String) {
        val uid = getUidSafe()
        if (uid.isBlank() || uid == "guest" || uid == AuthRepository.GUEST_UID) return
        repositoryScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).collection("cars").document(carId).collection("reports").get().await()
                val results = snap.documents.mapNotNull { doc ->
                    doc.toCarReport()
                }.sortedByDescending { it.date }
                updateMapCache(reportsCache, carId) { results }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching reports: ${e.message}")
            }
        }
    }

    suspend fun addCarReport(carId: String, report: CarReport) {
        val item = if (report.id.isBlank()) report.copy(id = UUID.randomUUID().toString()) else report
        updateMapCache(reportsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    val dto = item.toRemote().copy(carId = carId)
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("reports").document(dto.id).set(dto).await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing addCarReport: ${e.message}")
                }
            }
        }
    }

    suspend fun deleteCarReport(carId: String, reportId: String) {
        updateMapCache(reportsCache, carId) { list -> list.filterNot { it.id == reportId } }

        val uid = getUidSafe()
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    firestore.collection("users").document(uid).collection("cars").document(carId).collection("reports").document(reportId).delete().await()
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing deleteCarReport: ${e.message}")
                }
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

private fun DocumentSnapshot.safeStringOrNull(camelCaseKey: String): String? {
    val snake = camelCaseKey.camelToSnake()
    return getString(camelCaseKey) ?: getString(snake) ?: get(camelCaseKey)?.toString() ?: get(snake)?.toString()
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
    if (v2 is Number) return v2.toDouble()
    return getString(camelCaseKey)?.toDoubleOrNull() ?: getString(snake)?.toDoubleOrNull() ?: defaultValue
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
    getDate(camelCaseKey)?.let { return it }
    getDate(snake)?.let { return it }
    safeLongOrNull(camelCaseKey)?.let { return Date(it) }
    safeLongOrNull(snake)?.let { return Date(it) }
    val s = safeStringOrNull(camelCaseKey) ?: safeStringOrNull(snake)
    if (!s.isNullOrBlank()) {
        try {
            return isoFormat.parse(s) ?: defaultDate
        } catch (e: Exception) {
            s.toLongOrNull()?.let { return Date(it) }
        }
    }
    return defaultDate
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
