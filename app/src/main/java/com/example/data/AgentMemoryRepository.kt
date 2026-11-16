package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AgentMemoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeGameProfiles(): Flow<List<GameProfile>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/game_profiles"
        emitAll(
            db.collection("users").document(uid).collection("game_profiles")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeResearchDocs(): Flow<List<ResearchDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/research_docs"
        emitAll(
            db.collection("users").document(uid).collection("research_docs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ResearchDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStateMemories(): Flow<List<StateMemory>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/state_memories"
        emitAll(
            db.collection("users").document(uid).collection("state_memories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StateMemory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatchEpisodes(): Flow<List<MatchEpisode>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/match_episodes"
        emitAll(
            db.collection("users").document(uid).collection("match_episodes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchEpisode::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveOrUpdateGameProfile(profile: GameProfile): Result<String> {
        val uid = requireUserId()
        val docId = profile.gameId
        val docRef = db.collection("users").document(uid).collection("game_profiles").document(docId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleStrategicObjective(
        gameId: String,
        objectiveText: String,
        baseProfile: GameProfile
    ): Result<List<String>> {
        val uid = requireUserId()
        val normalizedObjective = objectiveText.trim().take(300)
        if (normalizedObjective.isBlank()) {
            return Result.success(baseProfile.completedObjectives)
        }
        val currentList = baseProfile.completedObjectives
        val updatedList = if (normalizedObjective in currentList) {
            currentList.filterNot { it == normalizedObjective }
        } else {
            (currentList + normalizedObjective).distinct().take(50)
        }
        val updatedProfile = baseProfile.copy(
            userId = uid,
            gameId = gameId,
            completedObjectives = updatedList
        )
        return saveOrUpdateGameProfile(updatedProfile).map { updatedList }
    }

    suspend fun getGameProfileByUser(ownerUid: String, gameId: String): Result<GameProfile?> {
        val docRef = db.collection("users").document(ownerUid).collection("game_profiles").document(gameId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    GameProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addResearchDoc(doc: ResearchDoc): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("research_docs").document()
        return try {
            docRef.set(doc.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertStateMemory(memory: StateMemory): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("state_memories")
        return try {
            if (memory.id.isNotBlank()) {
                val docRef = collectionRef.document(memory.id)
                docRef.update(memory.toUpdateMap()).await()
                Result.success(memory.id)
            } else {
                val docRef = collectionRef.document()
                docRef.set(memory.toCreateMap(uid)).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/state_memories")
            Result.failure(e)
        }
    }

    suspend fun deleteStateMemory(memoryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("state_memories").document(memoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addMatchEpisode(episode: MatchEpisode): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("match_episodes").document()
        return try {
            docRef.set(episode.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
