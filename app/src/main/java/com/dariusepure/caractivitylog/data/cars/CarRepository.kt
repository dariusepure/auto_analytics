package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import com.dariusepure.caractivitylog.domain.CarReport
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        repositoryScope.launch {
            authRepository.signedIn.collect {
                if (authRepository.isGuestMode) {
                    firestore.disableNetwork()
                } else {
                    firestore.enableNetwork()
                }
            }
        }
    }

    private fun getUid(): String {
        return authRepository.getUserId() ?: throw Exception("Utilizatorul nu este logat!")
    }

    val cars: Flow<List<Car>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreCar::class.java)?.fromFirebase()
                } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createCar(car: Car) {
        val uid = getUid()
        val firestoreCar = car.toFirebase()

        val reference = if (car.id.isEmpty()) {
            firestore.collection("users")
                .document(uid)
                .collection("cars")
                .document()
        } else {
            firestore.collection("users")
                .document(uid)
                .collection("cars")
                .document(car.id)
        }

        reference.set(firestoreCar).await()
    }

    fun getCarFlow(carId: String): Flow<Car?> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val car = snapshot?.toObject(FirestoreCar::class.java)?.fromFirebase()
                trySend(car)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getCar(carId: String): Car? {
        val uid = authRepository.getUserId() ?: return null
        return firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .get()
            .await()
            .toObject(FirestoreCar::class.java)
            ?.fromFirebase()
    }

    suspend fun deleteCar(carId: String) {
        val uid = getUid()
        
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .delete()
            .await()
    }

    fun getMileageLogs(carId: String): Flow<List<MileageLog>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("mileage")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreMileageLog::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addMileageLog(carId: String, log: MileageLog) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("mileage")
            .add(log.toFirebase())
            .await()
    }

    suspend fun updateMileageLog(carId: String, log: MileageLog) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("mileage")
            .document(log.id)
            .set(log.toFirebase())
            .await()
    }

    suspend fun deleteMileageLog(carId: String, logId: String) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("mileage")
            .document(logId)
            .delete()
            .await()
    }


    fun getInspections(carId: String): Flow<List<VehicleInspection>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("inspections")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreVehicleInspection::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUid()
        
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val inspectionRef = carRef.collection("inspections").document()

        val inspectionLog = inspection.copy(id = inspectionRef.id)

        inspectionRef.set(inspectionLog.toFirebase()).await()
    }


    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        carRef.collection("inspections").document(inspection.id)
            .set(inspection.toFirebase()).await()
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        carRef.collection("inspections").document(inspection.id).delete().await()
    }

    fun getInsurances(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Insurance>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("insurances")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreInsurance::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addInsurance(carId: String, insurance: com.dariusepure.caractivitylog.domain.Insurance) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("insurances")
            .add(insurance.toFirebase())
            .await()
    }

    suspend fun updateInsurance(carId: String, insurance: com.dariusepure.caractivitylog.domain.Insurance) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("insurances")
            .document(insurance.id)
            .set(insurance.toFirebase())
            .await()
    }

    suspend fun deleteInsurance(carId: String, insuranceId: String) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("insurances")
            .document(insuranceId)
            .delete()
            .await()
    }

    fun getVignettes(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Vignette>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("vignettes")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreVignette::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addVignette(carId: String, vignette: com.dariusepure.caractivitylog.domain.Vignette) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("vignettes")
            .add(vignette.toFirebase())
            .await()
    }

    suspend fun updateVignette(carId: String, vignette: com.dariusepure.caractivitylog.domain.Vignette) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("vignettes")
            .document(vignette.id)
            .set(vignette.toFirebase())
            .await()
    }

    suspend fun deleteVignette(carId: String, vignetteId: String) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("vignettes")
            .document(vignetteId)
            .delete()
            .await()
    }

    fun getTireSets(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.TireSet>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("tire_sets")
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreTireSet::class.java)
                    ?.map { it.fromFirebase() }
                    ?.sortedByDescending { it.isActive } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTireSet(carId: String, tireSet: com.dariusepure.caractivitylog.domain.TireSet) {
        val uid = getUid()
        
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val tireSetRef = carRef.collection("tire_sets").document()
        
        if (tireSet.isActive) {
            val otherSets = carRef.collection("tire_sets").whereEqualTo("isActive", true).get().await()
            firestore.runBatch { batch ->
                otherSets.documents.forEach { batch.update(it.reference, "isActive", false) }
                batch.set(tireSetRef, tireSet.copy(id = tireSetRef.id).toFirebase())
            }.await()
        } else {
            tireSetRef.set(tireSet.copy(id = tireSetRef.id).toFirebase()).await()
        }
    }

    suspend fun updateTireSet(carId: String, tireSet: com.dariusepure.caractivitylog.domain.TireSet) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val tireSetRef = carRef.collection("tire_sets").document(tireSet.id)
        
        if (tireSet.isActive) {
            val otherSets = carRef.collection("tire_sets").whereEqualTo("isActive", true).get().await()
            firestore.runBatch { batch ->
                otherSets.documents.forEach { 
                    if (it.id != tireSet.id) batch.update(it.reference, "isActive", false) 
                }
                batch.set(tireSetRef, tireSet.toFirebase())
            }.await()
        } else {
            tireSetRef.set(tireSet.toFirebase()).await()
        }
    }

    suspend fun deleteTireSet(carId: String, tireSetId: String) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("tire_sets")
            .document(tireSetId)
            .delete()
            .await()
    }

    fun getDiagnosisMessages(carId: String): Flow<List<ChatMessage>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("diagnosis")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreChatMessage::class.java)
                    ?.map { it.toChatMessage() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addDiagnosisMessage(carId: String, message: ChatMessage) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("diagnosis")
            .add(FirestoreChatMessage.fromChatMessage(message))
            .await()
    }

    suspend fun clearDiagnosisMessages(carId: String) {
        val uid = getUid()
        val collection = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("diagnosis")
        
        val snapshots = collection.get().await()
        firestore.runBatch { batch ->
            snapshots.documents.forEach { batch.delete(it.reference) }
        }.await()
    }

    fun getFuelLogs(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.FuelLog>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("fuel_logs")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreFuelLog::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        val uid = getUid()
        
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val fuelLogRef = carRef.collection("fuel_logs").document()

        val fuelLog = log.copy(id = fuelLogRef.id)
        fuelLogRef.set(fuelLog.toFirebase()).await()
    }

    suspend fun updateFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        carRef.collection("fuel_logs").document(log.id).set(log.toFirebase()).await()
    }

    suspend fun deleteFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        carRef.collection("fuel_logs").document(log.id).delete().await()
    }

    fun getMaintenanceLogs(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Maintenance>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("maintenance")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots
                    ?.toObjects(FirestoreMaintenance::class.java)
                    ?.map { it.fromFirebase() } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val maintenanceRef = carRef.collection("maintenance").document()

        val maintenanceLog = log.copy(id = maintenanceRef.id)
        maintenanceRef.set(maintenanceLog.toFirebase()).await()
    }

    suspend fun updateMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        carRef.collection("maintenance").document(log.id).set(log.toFirebase()).await()
    }

    suspend fun deleteMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        carRef.collection("maintenance").document(log.id).delete().await()
    }

    fun getCarReports(carId: String): Flow<List<CarReport>> = callbackFlow {
        val uid = authRepository.getUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("reports")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }

                val results = snapshots?.toObjects(FirestoreCarReport::class.java)
                    ?.map { it.toDomain(carId) } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addCarReport(carId: String, report: CarReport) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("reports")
            .add(report.toFirebase())
            .await()
    }

    suspend fun deleteCarReport(carId: String, reportId: String) {
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("reports")
            .document(reportId)
            .delete()
            .await()
    }
}

