package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.AiAnalysis
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
    private fun getUid(): String {
        return authRepository.getUserId() ?: throw Exception("Utilizatorul nu este logat!")
    }

    private fun checkNetwork() {
        if (authRepository.isGuestMode) {
            firestore.disableNetwork()
        } else {
            firestore.enableNetwork()
        }
    }

    val cars: Flow<List<Car>> = callbackFlow {
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
        val uid = getUid()
        
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .delete()
            .await()
    }

    fun getMileageLogs(carId: String): Flow<List<MileageLog>> = callbackFlow {
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
        val uid = getUid()
        
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val inspectionRef = carRef.collection("inspections").document()
        val mileageLogRef = carRef.collection("mileage").document()

        val mileageLog = MileageLog(id = mileageLogRef.id, km = inspection.mileage, date = inspection.date)
        val inspectionLog = inspection.copy(id = inspectionRef.id, mileageLogId = mileageLogRef.id)

        firestore.runBatch { batch ->
            batch.set(inspectionRef, inspectionLog.toFirebase())
            batch.set(mileageLogRef, mileageLog.toFirebase())
        }.await()
    }


    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.set(carRef.collection("inspections").document(inspection.id), inspection.toFirebase())
            if (inspection.mileageLogId.isNotEmpty()) {
                val mileageLog = MileageLog(id = inspection.mileageLogId, km = inspection.mileage, date = inspection.date)
                batch.set(carRef.collection("mileage").document(inspection.mileageLogId), mileageLog.toFirebase())
            }
        }.await()
    }

    suspend fun deleteInspection(carId: String, inspection: VehicleInspection) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.delete(carRef.collection("inspections").document(inspection.id))
            if (inspection.mileageLogId.isNotEmpty()) {
                batch.delete(carRef.collection("mileage").document(inspection.mileageLogId))
            }
        }.await()
    }

    fun getInsurances(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Insurance>> = callbackFlow {
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
        val uid = getUid()
        
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        val fuelLogRef = carRef.collection("fuel_logs").document()
        val mileageLogRef = carRef.collection("mileage").document()

        val mileageLog = MileageLog(id = mileageLogRef.id, km = log.km, date = log.date)
        val fuelLog = log.copy(id = fuelLogRef.id, mileageLogId = mileageLogRef.id)

        firestore.runBatch { batch ->
            batch.set(fuelLogRef, fuelLog.toFirebase())
            batch.set(mileageLogRef, mileageLog.toFirebase())
        }.await()
    }

    suspend fun updateFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.set(carRef.collection("fuel_logs").document(log.id), log.toFirebase())
            if (log.mileageLogId.isNotEmpty()) {
                val mileageLog = MileageLog(id = log.mileageLogId, km = log.km, date = log.date)
                batch.set(carRef.collection("mileage").document(log.mileageLogId), mileageLog.toFirebase())
            }
        }.await()
    }

    suspend fun deleteFuelLog(carId: String, log: com.dariusepure.caractivitylog.domain.FuelLog) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.delete(carRef.collection("fuel_logs").document(log.id))
            if (log.mileageLogId.isNotEmpty()) {
                batch.delete(carRef.collection("mileage").document(log.mileageLogId))
            }
        }.await()
    }

    fun getMaintenanceLogs(carId: String): Flow<List<com.dariusepure.caractivitylog.domain.Maintenance>> = callbackFlow {
        checkNetwork()
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
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        val maintenanceRef = carRef.collection("maintenance").document()
        val mileageLogRef = carRef.collection("mileage").document()

        val mileageLog = MileageLog(id = mileageLogRef.id, km = log.km, date = log.date)
        val maintenanceLog = log.copy(id = maintenanceRef.id, mileageLogId = mileageLogRef.id)

        firestore.runBatch { batch ->
            batch.set(maintenanceRef, maintenanceLog.toFirebase())
            batch.set(mileageLogRef, mileageLog.toFirebase())
        }.await()
    }

    suspend fun updateMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.set(carRef.collection("maintenance").document(log.id), log.toFirebase())
            if (log.mileageLogId.isNotEmpty()) {
                val mileageLog = MileageLog(id = log.mileageLogId, km = log.km, date = log.date)
                batch.set(carRef.collection("mileage").document(log.mileageLogId), mileageLog.toFirebase())
            }
        }.await()
    }

    suspend fun deleteMaintenanceLog(carId: String, log: com.dariusepure.caractivitylog.domain.Maintenance) {
        checkNetwork()
        val uid = getUid()
        val carRef = firestore.collection("users").document(uid).collection("cars").document(carId)
        
        firestore.runBatch { batch ->
            batch.delete(carRef.collection("maintenance").document(log.id))
            if (log.mileageLogId.isNotEmpty()) {
                batch.delete(carRef.collection("mileage").document(log.mileageLogId))
            }
        }.await()
    }

    fun getAiAnalysis(carId: String): Flow<AiAnalysis?> = callbackFlow {
        checkNetwork()
        val uid = authRepository.getUserId() ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("ai_analysis")
            .document("latest")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val analysis = snapshot?.toObject(FirestoreAiAnalysis::class.java)?.fromFirebase(carId)
                trySend(analysis)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveAiAnalysis(carId: String, analysis: AiAnalysis) {
        checkNetwork()
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("ai_analysis")
            .document("latest")
            .set(analysis.toFirebase())
            .await()
    }

    fun getCarReports(carId: String): Flow<List<CarReport>> = callbackFlow {
        checkNetwork()
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
        checkNetwork()
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
        checkNetwork()
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
