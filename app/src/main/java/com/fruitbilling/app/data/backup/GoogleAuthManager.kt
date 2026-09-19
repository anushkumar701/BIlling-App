package com.fruitbilling.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

data class GoogleUserData(
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

/**
 * Google Sign-In Manager for user authentication and cloud identity.
 */
object GoogleAuthManager {

    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleUserData? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return GoogleUserData(
            displayName = account.displayName,
            email = account.email,
            photoUrl = account.photoUrl?.toString()
        )
    }

    fun getSignInIntent(context: Context): Intent {
        val client = getGoogleSignInClient(context)
        return client.signInIntent
    }

    fun handleSignInResult(data: Intent?): Result<GoogleUserData> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            Result.success(
                GoogleUserData(
                    displayName = account.displayName,
                    email = account.email,
                    photoUrl = account.photoUrl?.toString()
                )
            )
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        val client = getGoogleSignInClient(context)
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
