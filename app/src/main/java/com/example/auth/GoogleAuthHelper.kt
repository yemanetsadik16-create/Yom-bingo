package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    Log.w(TAG, "default_web_client_id missing for auto sign-in", e)
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val activity = context.findActivity() ?: context
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
    return
  }

  val activity = context.findActivity()
  if (activity == null) {
    onAuthError("Unable to open Google Sign-In dialog from this context.")
    return
  }

  scope.launch {
    try {
      val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
      val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

      val result = try {
        credentialManager.getCredential(activity, primaryRequest)
      } catch (e: GetCredentialCancellationException) {
        throw e
      } catch (primaryErr: GetCredentialException) {
        if (primaryErr is NoCredentialException ||
          primaryErr is GetCredentialUnsupportedException ||
          primaryErr is GetCredentialProviderConfigurationException
        ) {
          Log.w(TAG, "Primary SignInWithGoogle option unavailable, retrying with GetGoogleIdOption", primaryErr)
          val fallbackOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
          val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(fallbackOption)
            .build()
          credentialManager.getCredential(activity, fallbackRequest)
        } else {
          throw primaryErr
        }
      }

      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type returned.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      val msg = e.message.orEmpty()
      if (msg.contains("28444") || msg.contains("Developer console", ignoreCase = true) ||
        msg.contains("Caller not", ignoreCase = true)
      ) {
        onAuthError("Google Sign-In SHA-1 / OAuth configuration mismatch. Rebuild with debug.keystore or add your signing SHA-1 in Firebase Console.")
      } else {
        onAuthCancelled()
      }
    } catch (e: NoCredentialException) {
      Log.e(TAG, "No Google credential available", e)
      onAuthError("No Google account found on this device. Add a Google account in device Settings and try again.")
    } catch (e: GetCredentialProviderConfigurationException) {
      Log.e(TAG, "Credential provider configuration error", e)
      onAuthError("Google Play Services Credential Manager is not available or needs updating on this device.")
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      val raw = e.localizedMessage ?: "Google Sign-In failed."
      if (raw.contains("28444") || raw.contains("Developer console", ignoreCase = true)) {
        onAuthError("Google Sign-In SHA-1 / OAuth configuration mismatch. Add this APK's SHA-1 fingerprint in Firebase Console.")
      } else {
        onAuthError(raw)
      }
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
