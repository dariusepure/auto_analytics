package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import com.dariusepure.caractivitylog.domain.CarReport
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.auth.AuthEvent
import android.util.Log
import com.dariusepure.caractivitylog.data.auth.FirestoreUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    private fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun getUid(): String {
        return authRepository.getUserId() ?: throw Exception("Utilizatorul nu este logat!")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cars: Flow<List<Car>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                
                try {
                    val email = authRepository.currentUserEmail
                    Log.d("CarRepository", "Fetching cars for UID: $uid and Email: $email")
                    
                    // 1. Găsim toate ID-urile posibile pentru acest email în profiles
                    val userIds = if (email != null) {
                        try {
                            supabaseClient.postgrest["profiles"]
                                .select { filter { eq("email", email) } }
                                .decodeList<FirestoreUser>()
                                .map { it.id }
                                .toSet() + uid
                        } catch (e: Exception) {
                            Log.w("CarRepository", "Failed to fetch associated UIDs: ${e.message}")
                            setOf(uid)
                        }
                    } else setOf(uid)

                    Log.d("CarRepository", "Searching cars for all associated UIDs: $userIds")

                    // 2. Căutăm mașinile care aparțin oricăruia dintre aceste ID-uri
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
                    
                    Log.d("CarRepository", "Supabase raw response: ${response.data}")
                    
                    val results = response.decodeList<FirestoreCar>()
                        .map { it.fromFirebase() }
                    
                    Log.d("CarRepository", "Found ${results.size} cars total.")
                    emit(results)
                } catch (e: Exception) {
                    Log.e("CarRepository", "Error fetching cars: ${e.message}", e)
                    emit(emptyList())
                }
            }
        }

    suspend fun createCar(car: Car) {
        val uid = getUid()
        val supabaseCar = car.toFirebase().copy(userId = uid) // Note: using userId from DTO
        supabaseClient.postgrest["cars"].upsert(supabaseCar)
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCarFlow(carId: String): Flow<Car?> = kotlinx.coroutines.flow.combine(refreshTrigger, authRepository.userId) { _, uid -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(null)
                    return@flow
                }
                val car = getCar(carId)
                emit(car)
            }
        }

    suspend fun getCar(carId: String): Car? {
        val uid = authRepository.getUserId() ?: return null
        return try {
            supabaseClient.postgrest["cars"]
                .select {
                    filter {
                        eq("id", carId)      // PK al mașinii
                        eq("user_id", uid)   // FK al utilizatorului
                    }
                }
                .decodeSingleOrNull<FirestoreCar>()
                ?.fromFirebase()
        } catch (e: Exception) {
            Log.e("CarRepository", "Error getting car $carId: ${e.message}")
            null
        }
    }

    suspend fun isVinDuplicate(vin: String, excludeCarId: String?): Boolean {
        return try {
            val currentCars = cars.first()
            currentCars.any { it.vin.equals(vin.trim(), ignoreCase = true) && it.id != excludeCarId }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCar(carId: String) {
        val uid = getUid()
        supabaseClient.postgrest["cars"].delete {
            filter {
                eq("id", carId)
                eq("user_id", uid)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMileageLogs(carId: String): Flow<List<MileageLog>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["mileage"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreMileageLog>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["mileage"].insert(dto)
        refresh()
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["mileage"].upsert(dto)
        refresh()
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        supabaseClient.postgrest["mileage"].delete {
            filter {
                eq("id", logId)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getInspections(carId: String): Flow<List<VehicleInspection>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["inspections"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreVehicleInspection>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addInspection(carId: String, inspection: VehicleInspection) {
        val dto = inspection.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["inspections"].insert(dto)
        refresh()
    }

    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        val dto = inspection.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["inspections"].upsert(dto)
        refresh()
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        supabaseClient.postgrest["inspections"].delete {
            filter {
                eq("id", inspection.id)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getInsurances(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Insurance>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["insurances"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreInsurance>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addInsurance(carId: String, insurance: com.dariusepure.caractivitylog.domain.Insurance) {
        val dto = insurance.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["insurances"].insert(dto)
        refresh()
    }

    suspend fun updateInsurance(carId: String, insurance: com.dariusepure.caractivitylog.domain.Insurance) {
        val dto = insurance.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["insurances"].upsert(dto)
        refresh()
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        supabaseClient.postgrest["insurances"].delete {
            filter {
                eq("id", insuranceId)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getVignettes(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Vignette>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["vignettes"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreVignette>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addVignette(carId: String, vignette: com.dariusepure.caractivitylog.domain.Vignette) {
        val dto = vignette.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["vignettes"].insert(dto)
        refresh()
    }

    suspend fun updateVignette(carId: String, vignette: com.dariusepure.caractivitylog.domain.Vignette) {
        val dto = vignette.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["vignettes"].upsert(dto)
        refresh()
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        supabaseClient.postgrest["vignettes"].delete {
            filter {
                eq("id", vignetteId)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTireSets(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.TireSet>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["tire_sets"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreTireSet>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.isActive }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addTireSet(carId: String, tireSet: com.dariusepure.caractivitylog.domain.TireSet) {
        val dto = tireSet.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["tire_sets"].insert(dto)
        refresh()
    }

    suspend fun updateTireSet(carId: String, tireSet: com.dariusepure.caractivitylog.domain.TireSet) {
        val dto = tireSet.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["tire_sets"].upsert(dto)
        refresh()
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        supabaseClient.postgrest["tire_sets"].delete {
            filter {
                eq("id", tireSetId)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDiagnosisMessages(carId: String): Flow<List<ChatMessage>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["diagnosis"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreChatMessage>()
                        .map { it.toChatMessage() }
                        .sortedBy { it.timestamp }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        val dto = FirestoreChatMessage.fromChatMessage(message).copy(carId = carId)
        supabaseClient.postgrest["diagnosis"].insert(dto)
        refresh()
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        supabaseClient.postgrest["diagnosis"].delete {
            filter {
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFuelLogs(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.FuelLog>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["fuel_logs"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreFuelLog>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["fuel_logs"].insert(dto)
        refresh()
    }

    suspend fun updateFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["fuel_logs"].upsert(dto)
        refresh()
    }

    suspend fun deleteFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        supabaseClient.postgrest["fuel_logs"].delete {
            filter {
                eq("id", log.id)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMaintenanceLogs(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Maintenance>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["maintenance"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreMaintenance>()
                        .map { it.fromFirebase() }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["maintenance"].insert(dto)
        refresh()
    }

    suspend fun updateMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        val dto = log.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["maintenance"].upsert(dto)
        refresh()
    }

    suspend fun deleteMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        supabaseClient.postgrest["maintenance"].delete {
            filter {
                eq("id", log.id)
                eq("car_id", carId)
            }
        }
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCarReports(carId: String): Flow<List<CarReport>> = kotlinx.coroutines.flow.combine(
        refreshTrigger, 
        authRepository.userId,
        authRepository.authEvents.onStart { emit(AuthEvent.SyncCompleted) }
    ) { _, uid, _ -> uid }
        .flatMapLatest { uid ->
            flow {
                if (uid == null) {
                    emit(emptyList())
                    return@flow
                }
                try {
                    val results = supabaseClient.postgrest["reports"]
                        .select {
                            filter {
                                eq("car_id", carId)
                            }
                        }
                        .decodeList<FirestoreCarReport>()
                        .map { it.toDomain(carId) }
                        .sortedByDescending { it.date }
                    emit(results)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }

    suspend fun addCarReport(carId: String, report: CarReport) {
        val dto = report.toFirebase().copy(carId = carId)
        supabaseClient.postgrest["reports"].insert(dto)
        refresh()
    }

    suspend fun deleteCarReport(carId: String, reportId: String) {
        supabaseClient.postgrest["reports"].delete {
            filter {
                eq("id", reportId)
                eq("car_id", carId)
            }
        }
        refresh()
    }
}
