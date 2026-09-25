package com.dariusepure.caractivitylog.data.cars

import android.util.Log
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.auth.RemoteUser
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
    private val localStorageHelper: LocalStorageHelper
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-Memory & User-Scoped Local CSV Caches for Instant 0ms UI Updates & Account Isolation
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
        val initialUid = getUidSafe()
        switchUserCache(initialUid)
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun getUidSafe(): String {
        return authRepository.getUserId() ?: "guest"
    }

    private fun switchUserCache(uid: String) {
        carsCache.value = localStorageHelper.loadCars(uid)
        inspectionsCache.value = localStorageHelper.loadInspections(uid)
        fuelLogsCache.value = localStorageHelper.loadFuelLogs(uid)
        maintenanceLogsCache.value = localStorageHelper.loadMaintenanceLogs(uid)
        mileageLogsCache.value = localStorageHelper.loadMileageLogs(uid)
        insurancesCache.value = localStorageHelper.loadInsurances(uid)
        vignettesCache.value = localStorageHelper.loadVignettes(uid)
        tireSetsCache.value = localStorageHelper.loadTireSets(uid)
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
        carsCache.value = updateBlock(carsCache.value)
        localStorageHelper.saveCars(getUidSafe(), carsCache.value)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    val cars: Flow<List<Car>> = flow {
        emit(carsCache.value)
        repositoryScope.launch {
            try {
                authRepository.userId.collect { uid ->
                    val activeUid = uid ?: "guest"
                    switchUserCache(activeUid)
                    if (uid != null) {
                        syncAllAccountDataToLocalCsv(uid)
                    }
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Error observing userId: ${e.message}")
            }
        }
        carsCache.collect { emit(it) }
    }

    private fun syncAllAccountDataToLocalCsv(uid: String) {
        repositoryScope.launch {
            try {
                val email = authRepository.currentUserEmail
                val userIds = if (email != null) {
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
                            or {
                                userIds.forEach { id ->
                                    eq("user_id", id)
                                    eq("id", id)
                                }
                            }
                        }
                    }

                val remoteCars = response.decodeList<RemoteCar>().map { it.fromRemote() }
                if (remoteCars.isNotEmpty()) {
                    carsCache.value = remoteCars
                    localStorageHelper.saveCars(uid, remoteCars)
                }

                // 2. Fetch history records for every car and populate local CSV files for this user
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
                        }
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Sync tire_sets error for $carId: ${e.message}")
                    }
                }

                // Save all updated maps to user's private CSV files
                localStorageHelper.saveInspections(uid, inspectionsCache.value)
                localStorageHelper.saveFuelLogs(uid, fuelLogsCache.value)
                localStorageHelper.saveMaintenanceLogs(uid, maintenanceLogsCache.value)
                localStorageHelper.saveMileageLogs(uid, mileageLogsCache.value)
                localStorageHelper.saveInsurances(uid, insurancesCache.value)
                localStorageHelper.saveVignettes(uid, vignettesCache.value)
                localStorageHelper.saveTireSets(uid, tireSetsCache.value)

                Log.d("CarRepository", "Full user CSV sync completed for UID: $uid")
            } catch (e: Exception) {
                Log.e("CarRepository", "Error in syncAllAccountDataToLocalCsv: ${e.message}")
            }
        }
    }

    suspend fun createCar(car: Car) {
        val item = if (car.id.isBlank()) car.copy(id = UUID.randomUUID().toString()) else car
        // Optimistic Local Update
        updateCarsCache { list -> (list.filterNot { it.id == item.id } + item) }

        // Background Sync
        repositoryScope.launch {
            try {
                val uid = getUidSafe()
                val supabaseCar = item.toRemote().copy(userId = uid)
                supabaseClient.postgrest["cars"].upsert(supabaseCar)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing createCar: ${e.message}")
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["mileage_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteMileageLog>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(mileageLogsCache, carId) { results }
                localStorageHelper.saveMileageLogs(uid, mileageLogsCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching mileage_logs: ${e.message}")
            }
        }
    }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val uid = getUidSafe()
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveMileageLogs(uid, mileageLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["mileage_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMileageLog: ${e.message}")
            }
        }
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        val uid = getUidSafe()
        updateMapCache(mileageLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }
        localStorageHelper.saveMileageLogs(uid, mileageLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["mileage_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMileageLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        val uid = getUidSafe()
        updateMapCache(mileageLogsCache, carId) { list -> list.filterNot { it.id == logId } }
        localStorageHelper.saveMileageLogs(uid, mileageLogsCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["inspections"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteVehicleInspection>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(inspectionsCache, carId) { results }
                localStorageHelper.saveInspections(uid, inspectionsCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching inspections: ${e.message}")
            }
        }
    }

    suspend fun addInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUidSafe()
        val item = if (inspection.id.isBlank()) inspection.copy(id = UUID.randomUUID().toString()) else inspection
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveInspections(uid, inspectionsCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["inspections"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInspection: ${e.message}")
            }
        }
    }

    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUidSafe()
        updateMapCache(inspectionsCache, carId) { list ->
            (list.filterNot { it.id == inspection.id } + inspection).sortedByDescending { it.date }
        }
        localStorageHelper.saveInspections(uid, inspectionsCache.value)

        repositoryScope.launch {
            try {
                val dto = inspection.toRemote().copy(carId = carId)
                supabaseClient.postgrest["inspections"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInspection: ${e.message}")
            }
        }
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUidSafe()
        updateMapCache(inspectionsCache, carId) { list -> list.filterNot { it.id == inspection.id } }
        localStorageHelper.saveInspections(uid, inspectionsCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["insurances"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteInsurance>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(insurancesCache, carId) { results }
                localStorageHelper.saveInsurances(uid, insurancesCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching insurances: ${e.message}")
            }
        }
    }

    suspend fun addInsurance(carId: String, insurance: Insurance) {
        val uid = getUidSafe()
        val item = if (insurance.id.isBlank()) insurance.copy(id = UUID.randomUUID().toString()) else insurance
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveInsurances(uid, insurancesCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["insurances"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addInsurance: ${e.message}")
            }
        }
    }

    suspend fun updateInsurance(carId: String, insurance: Insurance) {
        val uid = getUidSafe()
        updateMapCache(insurancesCache, carId) { list ->
            (list.filterNot { it.id == insurance.id } + insurance).sortedByDescending { it.date }
        }
        localStorageHelper.saveInsurances(uid, insurancesCache.value)

        repositoryScope.launch {
            try {
                val dto = insurance.toRemote().copy(carId = carId)
                supabaseClient.postgrest["insurances"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateInsurance: ${e.message}")
            }
        }
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        val uid = getUidSafe()
        updateMapCache(insurancesCache, carId) { list -> list.filterNot { it.id == insuranceId } }
        localStorageHelper.saveInsurances(uid, insurancesCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["vignettes"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteVignette>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(vignettesCache, carId) { results }
                localStorageHelper.saveVignettes(uid, vignettesCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching vignettes: ${e.message}")
            }
        }
    }

    suspend fun addVignette(carId: String, vignette: Vignette) {
        val uid = getUidSafe()
        val item = if (vignette.id.isBlank()) vignette.copy(id = UUID.randomUUID().toString()) else vignette
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveVignettes(uid, vignettesCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["vignettes"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addVignette: ${e.message}")
            }
        }
    }

    suspend fun updateVignette(carId: String, vignette: Vignette) {
        val uid = getUidSafe()
        updateMapCache(vignettesCache, carId) { list ->
            (list.filterNot { it.id == vignette.id } + vignette).sortedByDescending { it.date }
        }
        localStorageHelper.saveVignettes(uid, vignettesCache.value)

        repositoryScope.launch {
            try {
                val dto = vignette.toRemote().copy(carId = carId)
                supabaseClient.postgrest["vignettes"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateVignette: ${e.message}")
            }
        }
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        val uid = getUidSafe()
        updateMapCache(vignettesCache, carId) { list -> list.filterNot { it.id == vignetteId } }
        localStorageHelper.saveVignettes(uid, vignettesCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["tire_sets"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteTireSet>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.isActive }
                updateMapCache(tireSetsCache, carId) { results }
                localStorageHelper.saveTireSets(uid, tireSetsCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching tire_sets: ${e.message}")
            }
        }
    }

    suspend fun addTireSet(carId: String, tireSet: TireSet) {
        val uid = getUidSafe()
        val item = if (tireSet.id.isBlank()) tireSet.copy(id = UUID.randomUUID().toString()) else tireSet
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.isActive }
        }
        localStorageHelper.saveTireSets(uid, tireSetsCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["tire_sets"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addTireSet: ${e.message}")
            }
        }
    }

    suspend fun updateTireSet(carId: String, tireSet: TireSet) {
        val uid = getUidSafe()
        updateMapCache(tireSetsCache, carId) { list ->
            (list.filterNot { it.id == tireSet.id } + tireSet).sortedByDescending { it.isActive }
        }
        localStorageHelper.saveTireSets(uid, tireSetsCache.value)

        repositoryScope.launch {
            try {
                val dto = tireSet.toRemote().copy(carId = carId)
                supabaseClient.postgrest["tire_sets"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateTireSet: ${e.message}")
            }
        }
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        val uid = getUidSafe()
        updateMapCache(tireSetsCache, carId) { list -> list.filterNot { it.id == tireSetId } }
        localStorageHelper.saveTireSets(uid, tireSetsCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["fuel_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteFuelLog>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(fuelLogsCache, carId) { results }
                localStorageHelper.saveFuelLogs(uid, fuelLogsCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching fuel_logs: ${e.message}")
            }
        }
    }

    suspend fun addFuelLog(carId: String, log: FuelLog) {
        val uid = getUidSafe()
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveFuelLogs(uid, fuelLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["fuel_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addFuelLog: ${e.message}")
            }
        }
    }

    suspend fun updateFuelLog(carId: String, log: FuelLog) {
        val uid = getUidSafe()
        updateMapCache(fuelLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }
        localStorageHelper.saveFuelLogs(uid, fuelLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["fuel_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateFuelLog: ${e.message}")
            }
        }
    }

    suspend fun deleteFuelLog(carId: String, log: FuelLog) {
        val uid = getUidSafe()
        updateMapCache(fuelLogsCache, carId) { list -> list.filterNot { it.id == log.id } }
        localStorageHelper.saveFuelLogs(uid, fuelLogsCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
        repositoryScope.launch {
            try {
                val results = supabaseClient.postgrest["maintenance_logs"]
                    .select { filter { eq("car_id", carId) } }
                    .decodeList<RemoteMaintenance>()
                    .map { it.fromRemote() }
                    .sortedByDescending { it.date }
                updateMapCache(maintenanceLogsCache, carId) { results }
                localStorageHelper.saveMaintenanceLogs(uid, maintenanceLogsCache.value)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error fetching maintenance_logs: ${e.message}")
            }
        }
    }

    suspend fun addMaintenanceLog(carId: String, log: Maintenance) {
        val uid = getUidSafe()
        val item = if (log.id.isBlank()) log.copy(id = UUID.randomUUID().toString()) else log
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == item.id } + item).sortedByDescending { it.date }
        }
        localStorageHelper.saveMaintenanceLogs(uid, maintenanceLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = item.toRemote().copy(carId = carId)
                supabaseClient.postgrest["maintenance_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing addMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun updateMaintenanceLog(carId: String, log: Maintenance) {
        val uid = getUidSafe()
        updateMapCache(maintenanceLogsCache, carId) { list ->
            (list.filterNot { it.id == log.id } + log).sortedByDescending { it.date }
        }
        localStorageHelper.saveMaintenanceLogs(uid, maintenanceLogsCache.value)

        repositoryScope.launch {
            try {
                val dto = log.toRemote().copy(carId = carId)
                supabaseClient.postgrest["maintenance_logs"].upsert(dto)
            } catch (e: Exception) {
                Log.e("CarRepository", "Error syncing updateMaintenanceLog: ${e.message}")
            }
        }
    }

    suspend fun deleteMaintenanceLog(carId: String, log: Maintenance) {
        val uid = getUidSafe()
        updateMapCache(maintenanceLogsCache, carId) { list -> list.filterNot { it.id == log.id } }
        localStorageHelper.saveMaintenanceLogs(uid, maintenanceLogsCache.value)

        repositoryScope.launch {
            try {
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
        val uid = authRepository.getUserId() ?: return
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
