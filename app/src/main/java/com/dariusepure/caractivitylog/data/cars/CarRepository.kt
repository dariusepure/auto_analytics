package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.User
import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
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

    val cars: Flow<List<Car>> = authRepository.signedIn.flatMapLatest { signedIn ->
        if (!signedIn) return@flatMapLatest flowOf(emptyList())
        
        val uid = authRepository.getUserId() ?: return@flatMapLatest flowOf(emptyList())
        checkNetwork()

        callbackFlow {
            // Support both owned cars (legacy and new) and shared cars
            // For now, let's stick to the subcollection for owned cars and add a "shared_cars" collection or similar
            // OR use a collection group query if we move ownerUid to the car document
            
            val listener = firestore.collectionGroup("cars")
                .where(
                    Filter.or(
                        Filter.equalTo("ownerUid", uid),
                        Filter.arrayContains("sharedWith", uid)
                    )
                )
                .addSnapshotListener { snapshots, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    val results = snapshots?.documents?.mapNotNull { doc ->
                        val car = doc.toObject(FirestoreCar::class.java)?.fromFirebase()
                        if (car?.deleted == false) car else null
                    } ?: emptyList()

                    trySend(results.sortedByDescending { it.updatedAt })
                }

            awaitClose { listener.remove() }
        }
    }

    val deletedCars: Flow<List<Car>> = authRepository.signedIn.flatMapLatest { signedIn ->
        if (!signedIn) return@flatMapLatest flowOf(emptyList())
        
        val uid = authRepository.getUserId() ?: return@flatMapLatest flowOf(emptyList())
        checkNetwork()

        callbackFlow {
            val listener = firestore.collectionGroup("cars")
                .whereEqualTo("ownerUid", uid)
                .whereEqualTo("deleted", true)
                .addSnapshotListener { snapshots, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    val results = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(FirestoreCar::class.java)?.fromFirebase()
                    } ?: emptyList()

                    trySend(results.sortedByDescending { it.updatedAt })
                }

            awaitClose { listener.remove() }
        }
    }

    suspend fun createCar(car: Car) {
        checkNetwork()
        val uid = getUid()
        val firestoreCar = car.copy(ownerUid = uid).toFirebase()

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
            .update(
                "deleted", true,
                "deletedAt", com.google.firebase.Timestamp.now(),
                "updatedAt", com.google.firebase.Timestamp.now()
            )
            .await()
    }

    suspend fun restoreCar(carId: String) {
        checkNetwork()
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .update(
                "deleted", false,
                "deletedAt", null,
                "updatedAt", com.google.firebase.Timestamp.now()
            )
            .await()
    }

    suspend fun permanentlyDeleteCar(carId: String) {
        checkNetwork()
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .delete()
            .await()
    }

    suspend fun shareCar(carId: String, friendUid: String) {
        val ownerUid = getUid()
        val carRef = firestore.collection("users").document(ownerUid).collection("cars").document(carId)
        val car = carRef.get().await().toObject(FirestoreCar::class.java) ?: throw Exception("Car not found")
        
        val newSharedWith = car.sharedWith.toMutableList()
        if (!newSharedWith.contains(friendUid)) {
            newSharedWith.add(friendUid)
            carRef.update("sharedWith", newSharedWith).await()
        }
    }

    suspend fun unshareCar(carId: String, friendUid: String) {
        val ownerUid = getUid()
        val carRef = firestore.collection("users").document(ownerUid).collection("cars").document(carId)
        val car = carRef.get().await().toObject(FirestoreCar::class.java) ?: throw Exception("Car not found")
        
        val newSharedWith = car.sharedWith.toMutableList()
        if (newSharedWith.remove(friendUid)) {
            carRef.update("sharedWith", newSharedWith).await()
        }
    }

    fun getFriends(): Flow<List<User>> = callbackFlow {
        val uid = getUid()
        val listener = firestore.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snapshots, _ ->
                val friendUids = snapshots?.documents?.map { it.id } ?: emptyList()
                if (friendUids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Fetch user profiles for these UIDs
                // Note: Firestore doesn't support 'in' queries with more than 10-30 items easily, 
                // but for friends it's usually okay.
                firestore.collection("users").whereIn("uid", friendUids).get()
                    .addOnSuccessListener { userSnapshots ->
                        val users = userSnapshots.documents.mapNotNull { it.toObject(User::class.java) }
                        trySend(users)
                    }
            }
        awaitClose { listener.remove() }
    }

    suspend fun findUserByUsername(username: String): User? {
        val usernameDoc = firestore.collection("usernames").document(username.lowercase()).get().await()
        if (!usernameDoc.exists()) return null
        
        val uid = usernameDoc.getString("uid") ?: return null
        return firestore.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    suspend fun addFriend(friendUid: String) {
        val uid = getUid()
        if (uid == friendUid) throw Exception("Cannot add yourself as friend")
        
        firestore.runBatch { batch ->
            batch.set(firestore.collection("users").document(uid).collection("friends").document(friendUid), mapOf("since" to com.google.firebase.Timestamp.now()))
            batch.set(firestore.collection("users").document(friendUid).collection("friends").document(uid), mapOf("since" to com.google.firebase.Timestamp.now()))
        }.await()
    }

    suspend fun getAllCarsOnce(): List<Car> {
        return try {
            cars.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestInspection(carId: String): VehicleInspection? {
        return getInspections(carId).first().maxByOrNull { it.date }
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
        
        // 1. Add Inspection
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("inspections")
            .add(inspection.toFirebase())
            .await()

        // 2. Automatically add to mileage history
        addMileageLog(carId, MileageLog(km = inspection.mileage, date = inspection.date))
    }


    suspend fun updateInspection(carId: String, inspection: VehicleInspection) {
        checkNetwork()
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("inspections")
            .document(inspection.id)
            .set(inspection.toFirebase())
            .await()
    }

    suspend fun deleteInspection(carId: String, inspectionId: String) {
        checkNetwork()
        val uid = getUid()
        firestore.collection("users")
            .document(uid)
            .collection("cars")
            .document(carId)
            .collection("inspections")
            .document(inspectionId)
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
}
