package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.ProjectMappers
import com.example.model.CloudProject
import com.example.model.Project
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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
import java.util.UUID

class FirestoreProjectRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocumentId(rawId: String): String {
        val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { UUID.randomUUID().toString() }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        photoUrl: String?
    ): Result<UserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val profile = UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.trim().ifBlank { "MotionCut Creator" },
                photoUrl = photoUrl
            )
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            val saved = docRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: profile
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = OperationType.WRITE
            handleFirestoreError(e, op, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String = requireUserId()): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val profile = snap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeCloudProjects(userId: String): Flow<List<CloudProject>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("projects")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun getCloudProjects(userId: String = requireUserId()): Result<List<CloudProject>> {
        val colRef = db.collection("users").document(userId).collection("projects")
        return try {
            val snap = colRef.get().await()
            val list = snap.toObjects(
                CloudProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getCloudProjectById(
        projectId: String,
        targetUserId: String = requireUserId()
    ): Result<CloudProject?> {
        val docRef = db.collection("users")
            .document(targetUserId)
            .collection("projects")
            .document(sanitizeDocumentId(projectId))
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                CloudProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upsertCloudProject(project: Project): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = sanitizeDocumentId(project.id)
        val docRef = db.collection("users").document(uid).collection("projects").document(docId)
        val cloudModel = CloudProject.fromProject(project, uid).copy(id = docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(cloudModel.toMetadataUpdateMap()).await()
            } else {
                docRef.set(cloudModel.toCreateMap(uid)).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun renameCloudProject(projectId: String, newTitle: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val trimmed = newTitle.trim().take(120)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Project title cannot be blank"))
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("projects")
            .document(sanitizeDocumentId(projectId))
        return try {
            docRef.update(
                mapOf(
                    "title" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudProject(projectId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("projects")
            .document(sanitizeDocumentId(projectId))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun seedStarterProjectsIfEmpty(): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("projects")
        return try {
            val snap = colRef.limit(1).get().await()
            if (!snap.isEmpty) {
                return Result.success(false)
            }
            val starters = ProjectMappers.getInitialEntities()
            for (entity in starters) {
                val docId = sanitizeDocumentId(entity.id)
                val cloud = CloudProject.fromEntity(entity, uid).copy(id = docId)
                colRef.document(docId).set(cloud.toCreateMap(uid)).await()
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path, auth)
            Result.failure(e)
        }
    }
}
