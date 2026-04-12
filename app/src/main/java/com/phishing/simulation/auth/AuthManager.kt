package com.phishing.simulation.auth

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.phishing.simulation.model.User
import kotlinx.coroutines.tasks.await

// ---------------------------------------------------------------------------
// Result type returned by every AuthManager function
// ---------------------------------------------------------------------------

sealed class AuthResult {
    /** Successful operation — carries the user's Role string (e.g. "Admin"/"Viewer"). */
    data class Success(val role: String) : AuthResult()

    /** Failed operation — carries a human-readable message safe to display in the UI. */
    data class Failure(val message: String) : AuthResult()
}

// ---------------------------------------------------------------------------
// AuthManager
// ---------------------------------------------------------------------------

class AuthManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCol = db.collection("Users")

    companion object {
        private const val TAG = "AuthManager"
        const val ROLE_ADMIN = "Admin"
        const val ROLE_VIEWER = "Viewer"
    }

    // -----------------------------------------------------------------------
    // REGISTER
    // -----------------------------------------------------------------------

    /**
     * Creates a new Firebase Auth account and writes the user profile to
     * the "Users" Firestore collection with a default "Viewer" role.
     *
     * The Firestore document ID is set to the Firebase Auth UID.
     *
     * @return [AuthResult.Success] with role "Viewer" on success, or
     *         [AuthResult.Failure] with a descriptive message on error.
     */
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        department: String
    ): AuthResult {
        return try {
            // Step 1 — create the Auth account
            val credential = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = credential.user?.uid
                ?: return AuthResult.Failure("Registration failed: unable to retrieve user ID.")

            // Step 2 — write Firestore profile with doc ID = UID
            val user = User(
                name = name,
                email = email,
                role = ROLE_VIEWER,
                department = department,
                fcmToken = "",
                createdAt = Timestamp.now()
            )
            usersCol.document(uid).set(user).await()

            AuthResult.Success(ROLE_VIEWER)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "registerUser — auth error [${e.errorCode}]", e)
            AuthResult.Failure(friendlyAuthError(e.errorCode))
        } catch (e: Exception) {
            Log.e(TAG, "registerUser — unexpected error", e)
            AuthResult.Failure(e.message ?: "Registration failed. Please try again.")
        }
    }

    // -----------------------------------------------------------------------
    // LOGIN
    // -----------------------------------------------------------------------

    /**
     * Signs in with email & password, then fetches the user's "Role" field
     * from Firestore to drive post-login navigation.
     *
     * @return [AuthResult.Success] carrying the role string, or
     *         [AuthResult.Failure] on any error.
     */
    suspend fun loginUser(email: String, password: String): AuthResult {
        return try {
            // Step 1 — authenticate
            val credential = auth.signInWithEmailAndPassword(email, password).await()
            val uid = credential.user?.uid
                ?: return AuthResult.Failure("Login failed: unable to retrieve user ID.")

            // Step 2 — fetch role from Firestore
            val snapshot = usersCol.document(uid).get().await()
            val role = snapshot.getString("Role") ?: ROLE_VIEWER

            AuthResult.Success(role)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "loginUser — auth error [${e.errorCode}]", e)
            AuthResult.Failure(friendlyAuthError(e.errorCode))
        } catch (e: Exception) {
            Log.e(TAG, "loginUser — unexpected error", e)
            AuthResult.Failure(e.message ?: "Login failed. Please try again.")
        }
    }

    // -----------------------------------------------------------------------
    // CURRENT USER
    // -----------------------------------------------------------------------

    /**
     * Returns the Role of the currently signed-in user by reading their
     * Firestore document. Returns null if no user is signed in, the document
     * does not exist, or the read fails.
     */
    suspend fun getCurrentUserRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersCol.document(uid).get().await()
            snapshot.getString("Role")
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserRole — error for uid=$uid", e)
            null
        }
    }

    /** Signs out the currently authenticated user. */
    fun signOut() = auth.signOut()

    /** Returns true when a Firebase Auth session is active. */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Translates Firebase Auth error codes into user-friendly strings.
     * Falls back gracefully for unknown codes.
     */
    private fun friendlyAuthError(errorCode: String): String = when (errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE"   -> "This email address is already registered."
        "ERROR_INVALID_EMAIL"          -> "The email address format is invalid."
        "ERROR_WEAK_PASSWORD"          -> "Password must be at least 6 characters."
        "ERROR_USER_NOT_FOUND"         -> "No account found with this email."
        "ERROR_WRONG_PASSWORD"         -> "Incorrect password. Please try again."
        "ERROR_INVALID_CREDENTIAL"     -> "Invalid credentials. Check your email and password."
        "ERROR_USER_DISABLED"          -> "This account has been disabled."
        "ERROR_TOO_MANY_REQUESTS"      -> "Too many failed attempts. Please try again later."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your internet connection."
        else                           -> "Authentication error. Please try again."
    }
}
