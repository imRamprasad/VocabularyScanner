
package com.hostel.vocabularyscanner.data.repository

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hostel.vocabularyscanner.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithGoogle(activity: Activity): FirebaseUser {

        val credentialManager = CredentialManager.create(activity)

        // 1. Configure Google account request
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(
                activity.getString(R.string.default_web_client_id)
            )
            .build()

        // 2. Create credential request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 3. Open Google account selection
        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        // 4. Check returned credential
        val credential = result.credential

        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Invalid Google credential")
        }

        // 5. Extract Google ID token
        val googleIdToken = GoogleIdTokenCredential
            .createFrom(credential.data)
            .idToken

        // 6. Exchange Google token for Firebase credential
        val firebaseCredential =
            GoogleAuthProvider.getCredential(googleIdToken, null)

        // 7. Sign in to Firebase
        return suspendCancellableCoroutine { continuation ->

            auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener {
                    auth.currentUser?.let { user ->
                        if (continuation.isActive) {
                            continuation.resume(user)
                        }
                    } ?: run {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Firebase user not found")
                            )
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
