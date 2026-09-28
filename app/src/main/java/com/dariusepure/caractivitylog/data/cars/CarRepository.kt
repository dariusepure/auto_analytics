package com.dariusepure.caractivitylog.data.cars

import android.util.Log
import com.dariusepure.caractivitylog.data.auth.AuthEvent
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.auth.RemoteUser
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
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Vignette
import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
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
        repositoryScope.launch {
            csvToRoomMigrator.migrateIfNeeded()

            val initialUid = getUidSafe()
            switchUserCache(initialUid)

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
                                val supabaseCar = gCar.toRemote().copy(userId = targetUid)
                                supabaseClient.postgrest["cars"].upsert(supabaseCar)
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
                authRepository.ensureProfileExists(uid, email ?: "")

                val userIds = if (!email.isNullOrBlank()) {
                    try {
                        supabaseClient.postgrest["profiles"]
                            .select { filter { eq("email", email) } }
                            .decodeList<RemoteUser>()
                            .map { it.id }
                            .toSet() + uid
                    } catch (e: Exception) {
                        setOf(uid)
                    }
                } else setOf(uid)

                // 1. Fetch all cars for current account
                val response = supabaseClient.postgrest["cars"]
                    .select {
                        filter {
                            isIn("user_id", userIds.toList())
                        }
                    }

                val remoteCars = try {
                    response.decodeList<RemoteCar>().map { it.fromRemote() }
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error decoding remote cars: ${e.message}", e)
                    emptyList()
                }

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
                        mergedCars.add(local)
                        repositoryScope.launch {
                            try {
                                authRepository.ensureProfileExists(uid, email ?: "")
                                val supabaseCar = local.toRemote().copy(userId = uid)
                                supabaseClient.postgrest["cars"].upsert(supabaseCar)
                                Log.d("CarRepository", "Synced local unsynced car ${local.id} to remote")
                            } catch (e: Exception) {
                                Log.e("CarRepository", "Error pushing local car ${local.id} to remote: ${e.message}")
                            }
                        }
                    } else {
                        val remote = remoteMap[local.id]
                        if (remote != null && remote.vin.isBlank() && local.vin.isNotBlank()) {
                            repositoryScope.launch {
                                try {
                                    val supabaseCar = local.toRemote().copy(userId = uid)
                                    supabaseClient.postgrest["cars"].upsert(supabaseCar)
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

                // 2. Fetch history records for every car and populate Room for this user
                remoteCars.forEach { car ->
                    val carId = car.id

                    // Inspections
                    try {
                        val ins = supabaseClient.postgrest["inspections"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteVehicleInspection>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (ins.isNotEmpty()) {
                            updateMapCache(inspectionsCache, carId) { ins }
                            vehicleInspectionDao.insertInspections(ins.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync inspections error for $carId: ${e.message}")
                    }

                    // Fuel logs
                    try {
                        val fuels = supabaseClient.postgrest["fuel_logs"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteFuelLog>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (fuels.isNotEmpty()) {
                            updateMapCache(fuelLogsCache, carId) { fuels }
                            fuelLogDao.insertFuelLogs(fuels.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync fuel_logs error for $carId: ${e.message}")
                    }

                    // Maintenance logs
                    try {
                        val maint = supabaseClient.postgrest["maintenance_logs"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteMaintenance>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (maint.isNotEmpty()) {
                            updateMapCache(maintenanceLogsCache, carId) { maint }
                            maintenanceDao.insertMaintenanceLogs(maint.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync maintenance_logs error for $carId: ${e.message}")
                    }

                    // Mileage logs
                    try {
                        val mileage = supabaseClient.postgrest["mileage_logs"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteMileageLog>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (mileage.isNotEmpty()) {
                            updateMapCache(mileageLogsCache, carId) { mileage }
                            mileageLogDao.insertMileageLogs(mileage.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync mileage_logs error for $carId: ${e.message}")
                    }

                    // Insurances
                    try {
                        val insu = supabaseClient.postgrest["insurances"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteInsurance>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (insu.isNotEmpty()) {
                            updateMapCache(insurancesCache, carId) { insu }
                            insuranceDao.insertInsurances(insu.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync insurances error for $carId: ${e.message}")
                    }

                    // Vignettes
                    try {
                        val vig = supabaseClient.postgrest["vignettes"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteVignette>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.date }
                        if (vig.isNotEmpty()) {
                            updateMapCache(vignettesCache, carId) { vig }
                            vignetteDao.insertVignettes(vig.map { it.toEntity(carId) })
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync vignettes error for $carId: ${e.message}")
                    }

                    // Tire sets
                    try {
                        val tires = supabaseClient.postgrest["tire_sets"]
                            .select { filter { eq("car_id", carId) } }
                            .decodeList<RemoteTireSet>()
                            .map { it.fromRemote() }
                            .sortedByDescending { it.isActive }
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

        // Optimistic Local Update (Instant in-memory + async Room insert)
        updateCarsCache { list -> (list.filterNot { it.id == item.id } + item) }

        // Remote Sync (Asynchronous in background scope)
        if (uid.isNotBlank() && uid != "guest" && uid != AuthRepository.GUEST_UID) {
            repositoryScope.launch {
                try {
                    val email = authRepository.currentUserEmail ?: ""
                    authRepository.ensureProfileExists(uid, email)

                    val supabaseCar = item.toRemote().copy(userId = uid)
                    Log.d("CarRepository", "createCar: upserting car ${supabaseCar.id} with vin='${supabaseCar.vin}'")
                    supabaseClient.postgrest["cars"].upsert(supabaseCar)
                    Log.d("CarRepository", "createCar synced successfully for $uid")
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error syncing createCar: ${e.message}", e)
                }
            }
        }
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

        return try {
            supabaseClient.postgrest["cars"]
                .select { filter { eq("id", carId) } }
                .decodeSingleOrNull<RemoteCar>()
                ?.fromRemote()
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

        repositoryScope.launch {
            try {
                carDao.deleteCar(carId)
                val uid = getUidSafe()
                supabaseClient.postgrest["cars"].delete {
                    filter {
                        eq("id", carId)
                        eq("user_id", uid)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error deleting car $carId: ${e.message}")
            }
        }
    }

    // MILEAGE LOGS
    fun getMileageLogs(carId: String): Flow<List<MileageLog>> {
        fetchMileageLogsFromRemote(carId)
        return mileageLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchMileageLogsFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["mileage_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteMileageLog>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                mileageLogDao.insertMileageLogs(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["mileage_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMileageLog: ${e.message}")
            }
        }
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                mileageLogDao.insertMileageLogs(listOf(log.toEntity(carId)))
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["mileage_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMileageLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        updateMapCache(mileageLogsCache, carId) { list -> list.filterNot { it.id == logId } }

        repositoryScope.launch {
            try {
                mileageLogDao.deleteMileageLog(logId)
                supabaseClient.postgrest["mileage_logs"].delete {
                    filter {
                        eq("id", logId)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteMileageLog: ${e.message}")
            }
        }
    }

    // INSPECTIONS (ITP)
    fun getInspections(carId: String): Flow<List<VehicleInspection>> {
        fetchInspectionsFromRemote(carId)
        return inspectionsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchInspectionsFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["inspections"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteVehicleInspection>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                vehicleInspectionDao.insertInspections(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["inspections"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInspection: ${e.message}")
            }
        }
    }

    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == inspection.id } + inspection).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                vehicleInspectionDao.insertInspections(listOf(inspection.toEntity(carId)))
                val dto = inspection.toRemote().copy(carId = carId)
                supabaseClient.postgrest["inspections"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInspection: ${e.message}")
            }
        }
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        updateMapCache(inspectionsCache, carId) { list -> list.filterNot { it.id == inspection.id } }

        repositoryScope.launch {
            try {
                vehicleInspectionDao.deleteInspection(inspection.id)
                supabaseClient.postgrest["inspections"].delete {
                    filter {
                        eq("id", inspection.id)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteInspection: ${e.message}")
            }
        }
    }

    // INSURANCES (RCA/CASCO)
    fun getInsurances(carId: String): Flow<List<Insurance>> {
        fetchInsurancesFromRemote(carId)
        return insurancesCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchInsurancesFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["insurances"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteInsurance>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                insuranceDao.insertInsurances(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["insurances"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInsurance: ${e.message}")
            }
        }
    }

    suspend fun updateInsurance(carId: String, insurance: Insurance) {
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == insurance.id } + insurance).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                insuranceDao.insertInsurances(listOf(insurance.toEntity(carId)))
                val dto = insurance.toRemote().copy(carId = carId)
                supabaseClient.postgrest["insurances"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInsurance: ${e.message}")
            }
        }
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        updateMapCache(insurancesCache, carId) { list -> list.filterNot { it.id == insuranceId } }

        repositoryScope.launch {
            try {
                insuranceDao.deleteInsurance(insuranceId)
                supabaseClient.postgrest["insurances"].delete {
                    filter {
                        eq("id", insuranceId)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteInsurance: ${e.message}")
            }
        }
    }

    // VIGNETTES
    fun getVignettes(carId: String): Flow<List<Vignette>> {
        fetchVignettesFromRemote(carId)
        return vignettesCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchVignettesFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["vignettes"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteVignette>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                vignetteDao.insertVignettes(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["vignettes"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addVignette: ${e.message}")
            }
        }
    }

    suspend fun updateVignette(carId: String, vignette: Vignette) {
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == vignette.id } + vignette).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                vignetteDao.insertVignettes(listOf(vignette.toEntity(carId)))
                val dto = vignette.toRemote().copy(carId = carId)
                supabaseClient.postgrest["vignettes"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateVignette: ${e.message}")
            }
        }
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        updateMapCache(vignettesCache, carId) { list -> list.filterNot { it.id == vignetteId } }

        repositoryScope.launch {
            try {
                vignetteDao.deleteVignette(vignetteId)
                supabaseClient.postgrest["vignettes"].delete {
                    filter {
                        eq("id", vignetteId)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteVignette: ${e.message}")
            }
        }
    }

    // TIRE SETS
    fun getTireSets(carId: String): Flow<List<TireSet>> {
        fetchTireSetsFromRemote(carId)
        return tireSetsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchTireSetsFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["tire_sets"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteTireSet>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.isActive }
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

        repositoryScope.launch {
            try {
                tireSetDao.insertTireSets(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["tire_sets"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addTireSet: ${e.message}")
            }
        }
    }

    suspend fun updateTireSet(carId: String, tireSet: TireSet) {
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == tireSet.id } + tireSet).sortedByDescending { it.isActive }
        }

        repositoryScope.launch {
            try {
                tireSetDao.insertTireSets(listOf(tireSet.toEntity(carId)))
                val dto = tireSet.toRemote().copy(carId = carId)
                supabaseClient.postgrest["tire_sets"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateTireSet: ${e.message}")
            }
        }
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        updateMapCache(tireSetsCache, carId) { list -> list.filterNot { it.id == tireSetId } }

        repositoryScope.launch {
            try {
                tireSetDao.deleteTireSet(tireSetId)
                supabaseClient.postgrest["tire_sets"].delete {
                    filter {
                        eq("id", tireSetId)
                        eq("car_id", carId)
                    }
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
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["diagnosis_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteChatMessage>()
                    .map { it.toChatMessage() }
                    .sortedBy { it.timestamp }
                updateMapCache(diagnosisCache, carId) { results }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching diagnosis_logs: ${e.message}")
            }
        }
    }

    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        updateMapCache(diagnosisCache, carId) { list -> list + message }

        repositoryScope.launch {
            try {
                val dto = RemoteChatMessage.fromChatMessage(message).copy(carId = carId)
                supabaseClient.postgrest["diagnosis_logs"].insert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addDiagnosisMessage: ${e.message}")
            }
        }
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        updateMapCache(diagnosisCache, carId) { emptyList() }

        repositoryScope.launch {
            try {
                supabaseClient.postgrest["diagnosis_logs"].delete {
                    filter { eq("car_id", carId) }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error clearing diagnosis_logs: ${e.message}")
            }
        }
    }

    // FUEL LOGS
    fun getFuelLogs(carId: String): Flow<List<FuelLog>> {
        fetchFuelLogsFromRemote(carId)
        return fuelLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchFuelLogsFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["fuel_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteFuelLog>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                fuelLogDao.insertFuelLogs(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["fuel_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addFuelLog: ${e.message}")
            }
        }
    }

    suspend fun updateFuelLog(carId: String, log: FuelLog) {
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                fuelLogDao.insertFuelLogs(listOf(log.toEntity(carId)))
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["fuel_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateFuelLog: ${e.message}")
            }
        }
    }

    suspend fun deleteFuelLog(carId: String, log: FuelLog) {
        updateMapCache(fuelLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        repositoryScope.launch {
            try {
                fuelLogDao.deleteFuelLog(log.id)
                supabaseClient.postgrest["fuel_logs"].delete {
                    filter {
                        eq("id", log.id)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteFuelLog: ${e.message}")
            }
        }
    }

    // MAINTENANCE LOGS
    fun getMaintenanceLogs(carId: String): Flow<List<Maintenance>> {
        fetchMaintenanceLogsFromRemote(carId)
        return maintenanceLogsCache.map { map -> map[carId] ?: emptyList() }
    }

    private fun fetchMaintenanceLogsFromRemote(carId: String) {
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["maintenance_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteMaintenance>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                maintenanceDao.insertMaintenanceLogs(listOf(item.toEntity(carId)))
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["maintenance_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun updateMaintenanceLog(carId: String, log: Maintenance) {
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }

        repositoryScope.launch {
            try {
                maintenanceDao.insertMaintenanceLogs(listOf(log.toEntity(carId)))
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["maintenance_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMaintenanceLog(carId: String, log: Maintenance) {
        updateMapCache(maintenanceLogsCache, carId) { list -> list.filterNot { it.id == log.id } }

        repositoryScope.launch {
            try {
                maintenanceDao.deleteMaintenanceLog(log.id)
                supabaseClient.postgrest["maintenance_logs"].delete {
                    filter {
                        eq("id", log.id)
                        eq("car_id", carId)
                    }
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
        if (authRepository.getUserId() == null) return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["reports"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteCarReport>()
                    .map { it.toDomain(carId) }
                    .sortedByDescending { it.date }
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

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["reports"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addCarReport: ${e.message}")
            }
        }
    }

    suspend fun deleteCarReport(carId: String, reportId: String) {
        updateMapCache(reportsCache, carId) { list -> list.filterNot { it.id == reportId } }

        repositoryScope.launch {
            try {
                supabaseClient.postgrest["reports"].delete {
                    filter {
                        eq("id", reportId)
                        eq("car_id", carId)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing deleteCarReport: ${e.message}")
            }
        }
    }
}
