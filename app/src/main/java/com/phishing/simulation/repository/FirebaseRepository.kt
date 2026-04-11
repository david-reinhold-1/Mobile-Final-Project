package com.phishing.simulation.repository

import android.util.Log
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phishing.simulation.model.Campaign
import com.phishing.simulation.model.Detection
import com.phishing.simulation.model.User
import kotlinx.coroutines.tasks.await

// ---------------------------------------------------------------------------
// Result wrapper — returned by every repository function so that callers
// never need to catch exceptions themselves.
// ---------------------------------------------------------------------------

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()

    /** Convenience: true when this is a Success */
    val isSuccess get() = this is Success

    /** Returns the wrapped value, or null on failure */
    fun getOrNull(): T? = if (this is Success) data else null
}

// ---------------------------------------------------------------------------
// Pagination wrapper returned by getCampaigns()
// ---------------------------------------------------------------------------

data class PageResult<T>(
    /** Items in the current page */
    val items: List<T>,
    /** Pass this into the next getCampaigns() call as lastDocument to fetch the next page.
     *  Null when the returned page is the last one. */
    val lastDocument: DocumentSnapshot?
)

// ---------------------------------------------------------------------------
// Repository
// ---------------------------------------------------------------------------

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // Collection references
    private val usersCol      = db.collection("Users")
    private val campaignsCol  = db.collection("Campaigns")
    private val detectionsCol = db.collection("Detections")
    private val messagesCol   = db.collection("Messages")

    companion object {
        private const val TAG = "FirebaseRepository"
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    /**
     * Writes a new user document to Firestore.
     *
     * @param uid  Firebase Auth UID — used as the Firestore document ID.
     * @param user The [User] object to persist.
     */
    suspend fun registerUser(uid: String, user: User): Result<Unit> {
        return try {
            usersCol.document(uid).set(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "registerUser failed for uid=$uid", e)
            Result.Failure(e)
        }
    }

    /**
     * Creates a new campaign document under the "Campaigns" collection.
     * If [campaign.id] is blank a new document ID is auto-generated and
     * stored back on the `id` field before writing.
     *
     * @return [Result.Success] with the final document ID on success.
     */
    suspend fun createCampaign(campaign: Campaign): Result<String> {
        return try {
            val docRef = if (campaign.id.isBlank()) {
                campaignsCol.document()
            } else {
                campaignsCol.document(campaign.id)
            }
            val campaignWithId = campaign.copy(id = docRef.id)
            docRef.set(campaignWithId).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createCampaign failed", e)
            Result.Failure(e)
        }
    }

    /**
     * Saves a detection (phishing click) event to Firestore.
     *
     * @return [Result.Success] with the auto-generated document ID on success.
     */
    suspend fun saveDetection(detection: Detection): Result<String> {
        return try {
            val docRef = detectionsCol.document()
            docRef.set(detection).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "saveDetection failed", e)
            Result.Failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------

    /**
     * Fetches a single user profile by UID.
     *
     * @return [Result.Success] with a nullable [User] (null if the document
     *         does not exist yet).
     */
    suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val snapshot = usersCol.document(uid).get().await()
            Result.Success(snapshot.toObject(User::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile failed for uid=$uid", e)
            Result.Failure(e)
        }
    }

    /**
     * Fetches a page of campaigns ordered by creation date (ascending).
     *
     * Usage — first page:
     * ```kotlin
     * val page1 = repo.getCampaigns(limit = 10)
     * ```
     * Usage — next page:
     * ```kotlin
     * val page2 = repo.getCampaigns(limit = 10, lastDocument = page1.getOrNull()?.lastDocument)
     * ```
     *
     * @param limit        Maximum number of documents to fetch.
     * @param lastDocument The last [DocumentSnapshot] from the previous page,
     *                     or null to start from the beginning.
     * @return [Result.Success] wrapping a [PageResult] that includes the list
     *         of campaigns and the cursor for the next page.
     */
    suspend fun getCampaigns(
        limit: Long,
        lastDocument: DocumentSnapshot? = null
    ): Result<PageResult<Campaign>> {
        return try {
            var query: Query = campaignsCol
                .orderBy("CreatedAt")
                .limit(limit)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            val campaigns = snapshot.toObjects(Campaign::class.java)
            // Expose the raw snapshot so the caller can pass it to the next page call
            val newLastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            Result.Success(PageResult(items = campaigns, lastDocument = newLastDoc))
        } catch (e: Exception) {
            Log.e(TAG, "getCampaigns failed", e)
            Result.Failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    /**
     * Partially updates a user's Firestore document.
     *
     * Pass only the fields that changed, e.g.:
     * ```kotlin
     * repo.updateUserProfile(uid, mapOf("Department" to "IT", "fcmToken" to newToken))
     * ```
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCol.document(uid).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateUserProfile failed for uid=$uid", e)
            Result.Failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    /**
     * Permanently deletes a campaign document.
     *
     * Note: associated Detection documents are NOT deleted here. Wire up a
     * Cloud Function if cascade-delete behaviour is required.
     */
    suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        return try {
            campaignsCol.document(campaignId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCampaign failed for id=$campaignId", e)
            Result.Failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // AGGREGATION — used by chart / analytics screens
    // -----------------------------------------------------------------------

    /**
     * Returns the total number of detections recorded for a given campaign.
     *
     * Uses Firestore's server-side [AggregateSource.SERVER] count query so
     * that no documents are downloaded — efficient even for large collections.
     *
     * @param campaignId The campaign whose detections should be counted.
     * @return [Result.Success] with the count as a [Long].
     */
    suspend fun getDetectionCount(campaignId: String): Result<Long> {
        return try {
            val countQuery = detectionsCol
                .whereEqualTo("CampaignId", campaignId)
                .count()

            val snapshot = countQuery.get(AggregateSource.SERVER).await()
            Result.Success(snapshot.count)
        } catch (e: Exception) {
            Log.e(TAG, "getDetectionCount failed for campaignId=$campaignId", e)
            Result.Failure(e)
        }
    }
}
