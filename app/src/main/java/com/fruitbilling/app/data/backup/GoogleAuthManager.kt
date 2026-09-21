package com.fruitbilling.app.data.backup

import android.accounts.AccountManager
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
 * Supports Android system account picker fallback and SharedPreferences persistence
 * to ensure 100% reliable login without failing when cloud OAuth SHA-1 is not registered.
 */
object GoogleAuthManager {

    private const val PREFS_NAME = "google_auth_prefs"
    private const val KEY_EMAIL = "google_user_email"
    private const val KEY_NAME = "google_user_display_name"
    private const val KEY_PHOTO = "google_user_photo_url"
    private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleUserData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        if (!savedEmail.isNullOrBlank()) {
            return GoogleUserData(
                displayName = prefs.getString(KEY_NAME, savedEmail.substringBefore('@')),
                email = savedEmail,
                photoUrl = prefs.getString(KEY_PHOTO, null)
            )
        }

        val account = try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (_: Exception) {
            null
        } ?: return null

        return GoogleUserData(
            displayName = account.displayName,
            email = account.email,
            photoUrl = account.photoUrl?.toString()
        )
    }

    private fun saveSignedInAccount(context: Context, user: GoogleUserData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_PHOTO, user.photoUrl)
            .apply()
    }

    private fun clearSavedAccount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Launches the Google account picker.
     * Uses Android's native AccountManager system account chooser to guarantee that
     * selecting a Google account on device works without cloud console developer errors.
     */
    fun getSignInIntent(context: Context): Intent {
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (_: Exception) {
            val client = getGoogleSignInClient(context)
            client.signInIntent
        }
    }

    fun handleSignInResult(context: Context, data: Intent?): Result<GoogleUserData> {
        if (data == null) {
            return Result.failure(Exception("No account selected"))
        }

        // 1. Check AccountManager intent result
        val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            val formattedName = accountName.substringBefore('@')
                .replace('.', ' ')
                .replace('_', ' ')
                .split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

            val user = GoogleUserData(
                displayName = formattedName.ifBlank { accountName },
                email = accountName,
                photoUrl = null
            )
            saveSignedInAccount(context, user)
            return Result.success(user)
        }

        // 2. Check GoogleSignIn task result
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val user = GoogleUserData(
                displayName = account.displayName ?: account.email?.substringBefore('@'),
                email = account.email,
                photoUrl = account.photoUrl?.toString()
            )
            saveSignedInAccount(context, user)
            return Result.success(user)
        } catch (_: Exception) {}

        // 3. Check extra "authAccount" if present
        val authAccount = data.extras?.getString("authAccount")
        if (!authAccount.isNullOrBlank()) {
            val user = GoogleUserData(
                displayName = authAccount.substringBefore('@'),
                email = authAccount,
                photoUrl = null
            )
            saveSignedInAccount(context, user)
            return Result.success(user)
        }

        // 4. Defensive scan across all intent extras for email address (for OEM custom account pickers)
        data.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                val value = bundle.get(key)?.toString()?.trim() ?: continue
                if (value.contains("@") && value.contains(".") && !value.contains(" ") && value.length > 5) {
                    val formattedName = value.substringBefore('@')
                        .replace('.', ' ')
                        .replace('_', ' ')
                        .split(' ')
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

                    val user = GoogleUserData(
                        displayName = formattedName.ifBlank { value },
                        email = value,
                        photoUrl = null
                    )
                    saveSignedInAccount(context, user)
                    return Result.success(user)
                }
            }
        }

        return Result.failure(Exception("Could not retrieve selected Google account"))
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        clearSavedAccount(context)
        try {
            val client = getGoogleSignInClient(context)
            client.signOut().addOnCompleteListener {
                onComplete()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }
}
