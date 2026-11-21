package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.CategoryEntry
import com.example.core.model.CreditTransaction
import com.example.core.model.DesignProject
import com.example.core.model.DesignTemplate
import com.example.core.model.ExportRecord
import com.example.core.model.FavoriteEntry
import com.example.core.model.FontEntry
import com.example.core.model.GraphicAsset
import com.example.core.model.NotificationItem
import com.example.core.model.ReportItem
import com.example.core.model.RewardAdLog
import com.example.core.model.UserCredits
import com.example.core.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

class CanvaAoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Utilizador deve iniciar sessão com Google antes de aceder ao Firestore.")

    private fun <T> Flow<T>.handleStreamError(
        operationType: OperationType,
        path: String,
        expectedUid: String? = null
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun ensureUserInitialized(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        return try {
            val userRef = db.collection("users").document(uid)
            val creditsRef = db.collection("user_credits").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Criador CANVA AO"),
                    "email" to (user.email ?: ""),
                    "photoUrl" to user.photoUrl?.toString(),
                    "role" to "user",
                    "blocked" to false,
                    "themeMode" to "dark",
                    "language" to "pt",
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                userRef.set(profilePayload).await()
            }

            val creditsSnap = creditsRef.get().await()
            if (!creditsSnap.exists()) {
                val creditsPayload = mapOf(
                    "userId" to uid,
                    "balance" to 50L,
                    "totalEarned" to 50L,
                    "totalSpent" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                creditsRef.set(creditsPayload).await()

                val txRef = db.collection("credit_transactions").document()
                txRef.set(
                    mapOf(
                        "userId" to uid,
                        "type" to "welcome",
                        "amount" to 50L,
                        "description" to "Bónus de boas-vindas CANVA AO",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid")
            Result.failure(e)
        }
    }

    // --- Real-time Observers ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.GET, path, uid)
        )
    }

    fun observeUserCredits(userId: String): Flow<UserCredits> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "user_credits/$userId"
        emitAll(
            db.collection("user_credits").document(userId)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserCredits::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserCredits(userId = userId, balance = 50L, totalEarned = 50L, totalSpent = 0L)
                }
                .handleStreamError(OperationType.GET, path, uid)
        )
    }

    fun observeCreditTransactions(userId: String): Flow<List<CreditTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("credit_transactions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreditTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "credit_transactions", uid)
        )
    }

    fun observeRewardAds(userId: String): Flow<List<RewardAdLog>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("reward_ads")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RewardAdLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "reward_ads", uid)
        )
    }

    fun observeExports(userId: String): Flow<List<ExportRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("exports")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExportRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "exports", uid)
        )
    }

    fun observeUserProjects(userId: String): Flow<List<DesignProject>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("projects")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DesignProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "projects", uid)
        )
    }

    fun observeTemplates(): Flow<List<DesignTemplate>> = flow {
        requireUserId()
        emitAll(
            db.collection("templates")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DesignTemplate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.published }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "templates")
        )
    }

    fun observeCategories(): Flow<List<CategoryEntry>> = flow {
        requireUserId()
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CategoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.active }
                        .sortedBy { it.sortOrder }
                }
                .handleStreamError(OperationType.LIST, "categories")
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FavoriteEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleStreamError(OperationType.LIST, "favorites", uid)
        )
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("notifications")
                .whereIn("userId", listOf(userId, "all"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleStreamError(OperationType.LIST, "notifications", uid)
        )
    }

    fun observeAssets(): Flow<List<GraphicAsset>> = flow {
        requireUserId()
        emitAll(
            db.collection("assets")
                .snapshots()
                .map { snap -> snap.toObjects(GraphicAsset::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "assets")
        )
    }

    fun observeFonts(): Flow<List<FontEntry>> = flow {
        requireUserId()
        emitAll(
            db.collection("fonts")
                .snapshots()
                .map { snap -> snap.toObjects(FontEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "fonts")
        )
    }

    // --- Profile & Account Actions ---

    suspend fun updateProfile(
        displayName: String,
        photoUrl: String?,
        themeMode: String,
        language: String,
        notificationsEnabled: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val validUid = requireUserId()
            val cleanName = displayName.trim().take(80).ifEmpty { "Criador CANVA AO" }
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "themeMode" to themeMode,
                "language" to language,
                "notificationsEnabled" to notificationsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                updates["photoUrl"] = photoUrl.take(500)
            }
            db.collection("users").document(validUid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountDataAndAuth(): Result<Unit> {
        val uid = auth.currentUser?.uid
        return try {
            val validUid = requireUserId()
            // Clean up user projects & favorites & credits & profile before deleting auth user
            val projectsSnap = db.collection("projects").whereEqualTo("userId", validUid).get().await()
            projectsSnap.documents.forEach { it.reference.delete().await() }
            val favSnap = db.collection("favorites").whereEqualTo("userId", validUid).get().await()
            favSnap.documents.forEach { it.reference.delete().await() }
            db.collection("user_credits").document(validUid).delete().await()
            db.collection("users").document(validUid).delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${uid ?: "unauthenticated"}")
            Result.failure(e)
        }
    }

    // --- Projects CRUD ---

    suspend fun saveProject(project: DesignProject): Result<String> {
        val uid = auth.currentUser?.uid
        val isCreate = project.id.isBlank()
        val docRef = if (isCreate) {
            db.collection("projects").document()
        } else {
            db.collection("projects").document(project.id)
        }
        return try {
            val validUid = requireUserId()
            val cleanTitle = project.title.trim().take(120).ifEmpty { "Design Sem Título" }
            val cleanCat = project.category.trim().take(50).ifEmpty { "Instagram" }
            val cleanFormat = project.formatName.trim().take(60).ifEmpty { "Personalizado" }
            val safeWidth = project.width.coerceIn(100L, 8000L)
            val safeHeight = project.height.coerceIn(100L, 8000L)
            val safeLayers = project.layers.take(60)
            val safeClips = project.videoClips.take(20)

            if (isCreate) {
                val payload = mapOf(
                    "userId" to validUid,
                    "title" to cleanTitle,
                    "category" to cleanCat,
                    "formatName" to cleanFormat,
                    "width" to safeWidth,
                    "height" to safeHeight,
                    "backgroundColorHex" to project.backgroundColorHex.take(20),
                    "backgroundGradientHex" to project.backgroundGradientHex?.take(20),
                    "layers" to safeLayers,
                    "videoClips" to safeClips,
                    "projectType" to project.projectType,
                    "favorite" to project.favorite,
                    "thumbnailText" to project.thumbnailText?.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "title" to cleanTitle,
                    "category" to cleanCat,
                    "formatName" to cleanFormat,
                    "width" to safeWidth,
                    "height" to safeHeight,
                    "backgroundColorHex" to project.backgroundColorHex.take(20),
                    "backgroundGradientHex" to project.backgroundGradientHex?.take(20),
                    "layers" to safeLayers,
                    "videoClips" to safeClips,
                    "projectType" to project.projectType,
                    "favorite" to project.favorite,
                    "thumbnailText" to project.thumbnailText?.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun duplicateProject(project: DesignProject): Result<String> {
        return saveProject(
            project.copy(
                id = "",
                title = "${project.title.take(105)} (Cópia)",
                createdAt = null,
                updatedAt = null
            )
        )
    }

    suspend fun renameProject(projectId: String, newTitle: String): Result<Unit> {
        val path = "projects/$projectId"
        return try {
            requireUserId()
            require(projectId.isNotBlank())
            val cleanTitle = newTitle.trim().take(120).ifEmpty { "Design" }
            db.collection("projects").document(projectId).update(
                mapOf(
                    "title" to cleanTitle,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleProjectFavorite(projectId: String, favorite: Boolean): Result<Unit> {
        val path = "projects/$projectId"
        return try {
            requireUserId()
            db.collection("projects").document(projectId).update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val path = "projects/$projectId"
        return try {
            requireUserId()
            require(projectId.isNotBlank())
            db.collection("projects").document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTemplateFavorite(templateId: String, currentlyFavorite: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docId = "${uid ?: "none"}_$templateId"
        val docRef = db.collection("favorites").document(docId)
        return try {
            val validUid = requireUserId()
            val realRef = db.collection("favorites").document("${validUid}_$templateId")
            if (currentlyFavorite) {
                realRef.delete().await()
            } else {
                realRef.set(
                    mapOf(
                        "userId" to validUid,
                        "itemId" to templateId,
                        "itemType" to "template",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Credits & Rewarded Ads (Atomic + Idempotent) ---

    suspend fun claimRewardedAdCredits(adUnitId: String, idempotencyKey: String): Result<Long> {
        val uid = auth.currentUser?.uid
        return try {
            val validUid = requireUserId()
            val adDocId = "ad_${validUid}_${idempotencyKey.take(64).replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
            val adRef = db.collection("reward_ads").document(adDocId)
            val creditsRef = db.collection("user_credits").document(validUid)
            val txRef = db.collection("credit_transactions").document("tx_${adDocId}")

            val newBalance = db.runTransaction { txn ->
                val existingAd = txn.get(adRef)
                if (existingAd.exists()) {
                    throw IllegalStateException("Recompensa já processada para este anúncio.")
                }
                val creditSnap = txn.get(creditsRef)
                val currentBalance = creditSnap.getLong("balance") ?: 0L
                val totalEarned = creditSnap.getLong("totalEarned") ?: 0L
                val updatedBalance = currentBalance + 2L

                txn.set(
                    adRef,
                    mapOf(
                        "userId" to validUid,
                        "adUnitId" to adUnitId,
                        "rewardAmount" to 2L,
                        "status" to "completed",
                        "idempotencyKey" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.update(
                    creditsRef,
                    mapOf(
                        "balance" to updatedBalance,
                        "totalEarned" to (totalEarned + 2L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.set(
                    txRef,
                    mapOf(
                        "userId" to validUid,
                        "type" to "reward_ad",
                        "amount" to 2L,
                        "description" to "Anúncio recompensado concluído (+2 créditos)",
                        "referenceId" to adDocId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                updatedBalance
            }.await()
            Result.success(newBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "user_credits/${uid ?: "unauthenticated"}")
            Result.failure(e)
        }
    }

    // --- Export Reservation, Atomic Deduction & Failure Rollback ---

    suspend fun reserveExport(
        projectId: String,
        projectTitle: String,
        exportType: String,
        resolution: String,
        creditsCost: Long,
        fileName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid
        val exportId = "exp_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val exportRef = db.collection("exports").document(exportId)
        return try {
            val validUid = requireUserId()
            val creditsSnap = db.collection("user_credits").document(validUid).get().await()
            val balance = creditsSnap.getLong("balance") ?: 0L
            if (balance < creditsCost) {
                return Result.failure(
                    IllegalStateException("Créditos insuficientes. Necessário: $creditsCost, Saldo atual: $balance.")
                )
            }
            exportRef.set(
                mapOf(
                    "userId" to validUid,
                    "projectId" to projectId.ifBlank { "quick_export" },
                    "projectTitle" to projectTitle.trim().take(120).ifEmpty { "Design CANVA AO" },
                    "exportType" to exportType,
                    "resolution" to resolution.take(30),
                    "creditsCost" to creditsCost,
                    "status" to "pending",
                    "fileName" to fileName.take(160),
                    "fileSizeBytes" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(exportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "exports/$exportId")
            Result.failure(e)
        }
    }

    suspend fun completeExportWithCreditDeduction(
        exportId: String,
        creditsCost: Long,
        exportType: String,
        fileSizeBytes: Long
    ): Result<Long> {
        val uid = auth.currentUser?.uid
        return try {
            val validUid = requireUserId()
            val exportRef = db.collection("exports").document(exportId)
            val creditsRef = db.collection("user_credits").document(validUid)
            val txRef = db.collection("credit_transactions").document("tx_$exportId")

            val newBalance = db.runTransaction { txn ->
                val expSnap = txn.get(exportRef)
                if (!expSnap.exists() || expSnap.getString("status") != "pending") {
                    throw IllegalStateException("Exportação já finalizada ou inválida.")
                }
                val creditSnap = txn.get(creditsRef)
                val currentBalance = creditSnap.getLong("balance") ?: 0L
                val totalSpent = creditSnap.getLong("totalSpent") ?: 0L
                if (currentBalance < creditsCost) {
                    throw IllegalStateException("Saldo insuficiente para concluir a exportação.")
                }
                val updatedBalance = currentBalance - creditsCost

                txn.update(
                    exportRef,
                    mapOf(
                        "status" to "completed",
                        "fileSizeBytes" to fileSizeBytes.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.update(
                    creditsRef,
                    mapOf(
                        "balance" to updatedBalance,
                        "totalSpent" to (totalSpent + creditsCost),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txType = if (exportType == "video_mp4") "export_video" else "export_image"
                val desc = if (exportType == "video_mp4") {
                    "Exportação de Vídeo MP4 (-4 créditos)"
                } else {
                    "Exportação de Imagem (-2 créditos)"
                }
                txn.set(
                    txRef,
                    mapOf(
                        "userId" to validUid,
                        "type" to txType,
                        "amount" to -creditsCost,
                        "description" to desc,
                        "referenceId" to exportId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                updatedBalance
            }.await()
            Result.success(newBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "exports/$exportId")
            Result.failure(e)
        }
    }

    suspend fun markExportFailedOrCancelled(exportId: String, cancelled: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("exports").document(exportId).update(
                mapOf(
                    "status" to if (cancelled) "cancelled" else "failed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "exports/$exportId")
            Result.failure(e)
        }
    }

    suspend fun submitReport(targetId: String, reason: String): Result<Unit> {
        return try {
            val validUid = requireUserId()
            db.collection("reports").document().set(
                mapOf(
                    "userId" to validUid,
                    "targetId" to targetId.take(128).ifBlank { "app_feedback" },
                    "reason" to reason.trim().take(300),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            val validUid = requireUserId()
            val ref = db.collection("notifications").document(notificationId)
            val snap = ref.get().await()
            if (snap.exists() && snap.getString("userId") == validUid) {
                ref.update("read", true).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
            Result.failure(e)
        }
    }

    // --- Admin Operations (Protected by Firestore Security Rules isAdmin()) ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "users")
        )
    }

    fun observeAllExportsAdmin(): Flow<List<ExportRecord>> = flow {
        requireUserId()
        emitAll(
            db.collection("exports").snapshots()
                .map { snap -> snap.toObjects(ExportRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "exports")
        )
    }

    fun observeAllRewardAdsAdmin(): Flow<List<RewardAdLog>> = flow {
        requireUserId()
        emitAll(
            db.collection("reward_ads").snapshots()
                .map { snap -> snap.toObjects(RewardAdLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "reward_ads")
        )
    }

    fun observeAllReportsAdmin(): Flow<List<ReportItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("reports").snapshots()
                .map { snap -> snap.toObjects(ReportItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleStreamError(OperationType.LIST, "reports")
        )
    }

    suspend fun toggleUserBlockAdmin(targetUserId: String, blocked: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("users").document(targetUserId).update(
                mapOf("blocked" to blocked, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun grantCreditsAdmin(targetUserId: String, amount: Long): Result<Unit> {
        return try {
            requireUserId()
            val creditsRef = db.collection("user_credits").document(targetUserId)
            val txRef = db.collection("credit_transactions").document()
            db.runTransaction { txn ->
                val snap = txn.get(creditsRef)
                val bal = snap.getLong("balance") ?: 0L
                val earned = snap.getLong("totalEarned") ?: 0L
                txn.update(
                    creditsRef,
                    mapOf(
                        "balance" to (bal + amount).coerceAtLeast(0L),
                        "totalEarned" to (earned + amount.coerceAtLeast(0L)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.set(
                    txRef,
                    mapOf(
                        "userId" to targetUserId,
                        "type" to "admin_grant",
                        "amount" to amount,
                        "description" to "Ajuste administrativo de créditos",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "user_credits/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun saveTemplateAdmin(template: DesignTemplate): Result<Unit> {
        val docRef = if (template.id.isBlank()) {
            db.collection("templates").document()
        } else {
            db.collection("templates").document(template.id)
        }
        return try {
            requireUserId()
            if (template.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "name" to template.name.trim().take(100),
                        "category" to template.category.trim().take(50),
                        "previewStyle" to template.previewStyle.take(60),
                        "width" to template.width,
                        "height" to template.height,
                        "backgroundColorHex" to template.backgroundColorHex.take(20),
                        "backgroundGradientHex" to template.backgroundGradientHex?.take(20),
                        "layers" to template.layers.take(50),
                        "fontsUsed" to template.fontsUsed.take(10),
                        "colorsUsed" to template.colorsUsed.take(10),
                        "premium" to template.premium,
                        "published" to template.published,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null } as Map<String, Any>
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to template.name.trim().take(100),
                        "category" to template.category.trim().take(50),
                        "previewStyle" to template.previewStyle.take(60),
                        "width" to template.width,
                        "height" to template.height,
                        "backgroundColorHex" to template.backgroundColorHex.take(20),
                        "backgroundGradientHex" to template.backgroundGradientHex?.take(20),
                        "layers" to template.layers.take(50),
                        "fontsUsed" to template.fontsUsed.take(10),
                        "colorsUsed" to template.colorsUsed.take(10),
                        "premium" to template.premium,
                        "published" to template.published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null } as Map<String, Any>
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTemplateAdmin(templateId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("templates").document(templateId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "templates/$templateId")
            Result.failure(e)
        }
    }

    suspend fun createCategoryAdmin(name: String, iconKey: String, sortOrder: Long): Result<Unit> {
        return try {
            requireUserId()
            db.collection("categories").document().set(
                mapOf(
                    "name" to name.trim().take(50),
                    "iconKey" to iconKey.trim().take(40),
                    "sortOrder" to sortOrder,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "categories")
            Result.failure(e)
        }
    }

    suspend fun createAssetAdmin(name: String, assetType: String, category: String, colorHex: String, symbolKey: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("assets").document().set(
                mapOf(
                    "name" to name.trim().take(60),
                    "assetType" to assetType,
                    "category" to category.take(40),
                    "colorHex" to colorHex.take(20),
                    "symbolKey" to symbolKey.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "assets")
            Result.failure(e)
        }
    }

    suspend fun createFontAdmin(name: String, familyKey: String, category: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("fonts").document().set(
                mapOf(
                    "name" to name.trim().take(60),
                    "familyKey" to familyKey.trim().take(40),
                    "category" to category.trim().take(40),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "fonts")
            Result.failure(e)
        }
    }

    suspend fun broadcastNotificationAdmin(title: String, message: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("notifications").document().set(
                mapOf(
                    "userId" to "all",
                    "title" to title.trim().take(100),
                    "message" to message.trim().take(400),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun resolveReportAdmin(reportId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("reports").document(reportId).update(
                mapOf("status" to "resolved", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
            Result.failure(e)
        }
    }
}
