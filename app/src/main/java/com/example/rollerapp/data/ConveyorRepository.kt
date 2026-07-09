package com.example.rollerapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ConveyorRepository(private val dao: RollerDao) {
    val allConveyors: Flow<List<Conveyor>> = dao.getAllConveyors()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    suspend fun addConveyor(name: String, number: String = "") {
        dao.insertConveyor(Conveyor(name, number, lastModified = System.currentTimeMillis()))
    }

    suspend fun deleteConveyor(name: String) {
        dao.deleteConveyor(Conveyor(name))
        // Also delete from Firebase
        try {
            val user = auth.currentUser ?: return
            val uid = user.uid
            val conveyorRef = firestore.collection("users").document(uid)
                .collection("conveyors").document(name)
            
            // Delete subcollections
            val inspections = conveyorRef.collection("inspections").get().await().documents
            for (doc in inspections) doc.reference.delete().await()
            
            val replacements = conveyorRef.collection("replacements").get().await().documents
            for (doc in replacements) doc.reference.delete().await()
            
            val journal = conveyorRef.collection("journal").get().await().documents
            for (doc in journal) doc.reference.delete().await()
            
            // Delete conveyor doc
            conveyorRef.delete().await()
        } catch (e: Exception) {
            android.util.Log.e("DeleteConveyor", "Error deleting conveyor from Firebase", e)
        }
    }

    // Inspections
    fun getInspections(conveyor: String) = dao.getInspectionsByConveyor(conveyor)
    
    suspend fun addInspection(inspection: Inspection) {
        dao.insertInspection(inspection.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun updateInspection(inspection: Inspection) {
        dao.updateInspection(inspection.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun deleteInspection(inspection: Inspection, moveToHistory: Boolean) {
        if (moveToHistory) {
            dao.insertReplacement(
                Replacement(
                    conveyorId = inspection.conveyorId,
                    opora = inspection.opora,
                    roller = inspection.roller,
                    reason = inspection.damage,
                    timestamp = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        dao.deleteInspection(inspection)
        // Also delete from Firebase
        deleteFromFirebase("inspections", inspection.conveyorId, inspection.id.toString())
    }

    suspend fun deleteReplacement(replacement: Replacement) {
        dao.deleteReplacement(replacement)
        deleteFromFirebase("replacements", replacement.conveyorId, replacement.id.toString())
    }

    suspend fun deleteJournalEntry(entry: BeltJournal) {
        dao.deleteJournalEntry(entry)
        deleteFromFirebase("journal", entry.conveyorId, entry.id.toString())
    }

    // Replacements
    fun getReplacements(conveyor: String) = dao.getReplacementsByConveyor(conveyor)
    
    suspend fun addReplacement(replacement: Replacement) {
        dao.insertReplacement(replacement.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun updateReplacement(replacement: Replacement) {
        dao.updateReplacement(replacement.copy(lastModified = System.currentTimeMillis()))
    }

    // Journal
    fun getJournal(conveyor: String) = dao.getJournalByConveyor(conveyor)
    
    suspend fun addJournalEntry(entry: BeltJournal) {
        dao.insertJournalEntry(entry.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun updateJournalEntry(entry: BeltJournal) {
        dao.updateJournalEntry(entry.copy(lastModified = System.currentTimeMillis()))
    }

    private suspend fun deleteFromFirebase(collection: String, conveyorName: String, docId: String) {
        try {
            val user = auth.currentUser ?: return
            val uid = user.uid
            firestore.collection("users").document(uid)
                .collection("conveyors").document(conveyorName)
                .collection(collection).document(docId)
                .delete().await()
        } catch (e: Exception) {
            android.util.Log.e("DeleteFromFirebase", "Error deleting $collection/$docId", e)
        }
    }

    // --- Synchronization Logic ---

    suspend fun syncAll(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not signed in. Please sign in with email/password.")
            val uid = user.uid

            // Load local conveyors
            val localConveyors = allConveyors.first()

            // Load remote conveyors for this user and merge into local DB
            val remoteConveyorDocs = firestore.collection("users").document(uid)
                .collection("conveyors").get().await().documents

            val remoteConveyors = remoteConveyorDocs.mapNotNull { it.toObject(Conveyor::class.java) }

            // Insert or update any remote conveyors locally if they are new or newer
            for (remote in remoteConveyors) {
                val local = dao.getConveyorByName(remote.name)
                if (local == null || remote.lastModified > local.lastModified) {
                    dao.insertConveyor(remote)
                }
            }

            // Build union of conveyor names (local + remote) and sync each
            val names = (localConveyors.map { it.name } + remoteConveyors.map { it.name }).distinct()
            for (name in names) {
                syncConveyor(uid, name)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Authentication helpers for email/password ---
    suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val current = auth.currentUser
            val credential = EmailAuthProvider.getCredential(email, password)
            if (current != null && current.isAnonymous) {
                // Link anonymous account to new email account to preserve local data
                current.linkWithCredential(credential).await()
            } else {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val current = auth.currentUser
            val credential = EmailAuthProvider.getCredential(email, password)
            if (current != null && current.isAnonymous) {
                try {
                    // Try to link anonymous account to the provided credentials (preserve local data)
                    current.linkWithCredential(credential).await()
                } catch (linkEx: Exception) {
                    // If linking fails (for example account already exists), sign in to existing account.
                    auth.signInWithEmailAndPassword(email, password).await()
                }
            } else {
                auth.signInWithEmailAndPassword(email, password).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Admin remote operations
    suspend fun clearRemoteData(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val uid = user.uid
            val conveyors = firestore.collection("users").document(uid).collection("conveyors").get().await().documents
            for (doc in conveyors) {
                // delete subcollections
                val name = doc.id
                firestore.collection("users").document(uid).collection("conveyors").document(name).collection("inspections").get().await().documents.forEach { it.reference.delete().await() }
                firestore.collection("users").document(uid).collection("conveyors").document(name).collection("replacements").get().await().documents.forEach { it.reference.delete().await() }
                firestore.collection("users").document(uid).collection("conveyors").document(name).collection("journal").get().await().documents.forEach { it.reference.delete().await() }
                // delete conveyor doc
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeRemoteDuplicates(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            val uid = user.uid
            val convs = firestore.collection("users").document(uid).collection("conveyors").get().await().documents
            for (conv in convs) {
                val ins = conv.reference.collection("inspections").get().await().documents
                val seen = mutableSetOf<String>()
                for (doc in ins) {
                    val obj = doc.toObject(Inspection::class.java) ?: continue
                    val key = "${obj.conveyorId}_${obj.opora}_${obj.roller}"
                    if (seen.contains(key)) {
                        doc.reference.delete().await()
                    } else seen.add(key)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncConveyor(uid: String, conveyorName: String) {
        // 1. Sync Conveyor object itself
        val conveyorRef = firestore.collection("users").document(uid)
            .collection("conveyors").document(conveyorName)
        
        val remoteSnapshot = conveyorRef.get().await()
        val remoteConveyor = remoteSnapshot.toObject(Conveyor::class.java)
        val remoteNumber = remoteSnapshot.getString("number") // Загружаем номер конвейера из Firestore
        
        val localConveyor = dao.getConveyorByName(conveyorName)
        
        if (localConveyor != null) {
            if (remoteConveyor == null || localConveyor.lastModified > remoteConveyor.lastModified) {
                conveyorRef.set(localConveyor).await()
            } else if (remoteConveyor.lastModified > localConveyor.lastModified) {
                dao.insertConveyor(remoteConveyor)
            }
        }
        
        // Логирование загруженного номера конвейера
        if (remoteNumber != null) {
            android.util.Log.d("ConveyorSync", "Conveyor '$conveyorName' number loaded: '$remoteNumber'")
        }

        // 2. Sync Inspections
        syncCollection(
            uid = uid,
            conveyorName = conveyorName,
            subCollection = "inspections",
            localData = dao.getInspectionsByConveyorSync(conveyorName),
            remoteType = Inspection::class.java,
            onDownload = { dao.insertInspection(it) }
        )

        // 3. Sync Replacements
        syncCollection(
            uid = uid,
            conveyorName = conveyorName,
            subCollection = "replacements",
            localData = dao.getReplacementsByConveyorSync(conveyorName),
            remoteType = Replacement::class.java,
            onDownload = { dao.insertReplacement(it) }
        )

        // 4. Sync Journal
        syncCollection(
            uid = uid,
            conveyorName = conveyorName,
            subCollection = "journal",
            localData = dao.getJournalByConveyorSync(conveyorName),
            remoteType = BeltJournal::class.java,
            onDownload = { dao.insertJournalEntry(it) }
        )
    }

    private suspend fun <T : Any> syncCollection(
        uid: String,
        conveyorName: String,
        subCollection: String,
        localData: List<T>,
        remoteType: Class<T>,
        onDownload: suspend (T) -> Unit
    ) {
        val collectionRef = firestore.collection("users").document(uid)
            .collection("conveyors").document(conveyorName)
            .collection(subCollection)

        val remoteDocs = collectionRef.get().await().documents.mapNotNull { it.toObject(remoteType) }
        val localMap = localData.associateBy { getEntityId(it) }
        val remoteMap = remoteDocs.associateBy { getEntityId(it) }

        // Upload new or newer local data
        for (local in localData) {
            val id = getEntityId(local)
            val remote = remoteMap[id]
            if (remote == null || getEntityLastModified(local) > getEntityLastModified(remote)) {
                collectionRef.document(id.toString()).set(local).await()
            }
        }

        // Download new or newer remote data
        for (remote in remoteDocs) {
            val id = getEntityId(remote)
            val local = localMap[id]
            if (local == null || getEntityLastModified(remote) > getEntityLastModified(local)) {
                onDownload(remote)
            }
        }
    }

    private fun getEntityId(entity: Any): Any {
        return when (entity) {
            is Inspection -> entity.id
            is Replacement -> entity.id
            is BeltJournal -> entity.id
            is Conveyor -> entity.name
            else -> throw IllegalArgumentException("Unknown entity type")
        }
    }

    private fun getEntityLastModified(entity: Any): Long {
        return when (entity) {
            is Inspection -> entity.lastModified
            is Replacement -> entity.lastModified
            is BeltJournal -> entity.lastModified
            is Conveyor -> entity.lastModified
            else -> throw IllegalArgumentException("Unknown entity type")
        }
    }
}
