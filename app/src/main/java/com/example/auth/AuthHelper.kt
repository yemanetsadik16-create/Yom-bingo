package com.example.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "Auth"
private const val INTERNAL_EMAIL_DOMAIN = "yemebingo.internal"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

fun normalizeUsernameSlug(rawUsername: String): String =
  rawUsername.trim()
    .lowercase()
    .replace(Regex("\\s+"), ".")
    .replace(Regex("[^a-z0-9._-]"), "")
    .replace(Regex("\\.{2,}"), ".")
    .trim('.')

fun isValidRegistrationUsername(rawUsername: String): Boolean {
  val trimmed = rawUsername.trim()
  if (trimmed.length !in 2..40 || '@' in trimmed) return false
  return normalizeUsernameSlug(trimmed).length >= 2
}

fun usernameToInternalEmail(rawUsername: String): String {
  val trimmed = rawUsername.trim()
  if ('@' in trimmed) return trimmed.lowercase()
  val slug = normalizeUsernameSlug(trimmed)
  return "$slug@$INTERNAL_EMAIL_DOMAIN"
}

fun signInWithUsernameAndPassword(
  usernameOrEmail: String,
  password: String,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
) {
  val email = usernameToInternalEmail(usernameOrEmail)
  scope.launch {
    try {
      Firebase.auth.signInWithEmailAndPassword(email, password).await()
      onAuthSuccess()
    } catch (e: Exception) {
      Log.w(TAG, "Login failed for $email", e)
      onAuthError(mapFirebaseAuthError(e, isRegistration = false))
    }
  }
}

fun registerWithUsernameAndPassword(
  username: String,
  password: String,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
) {
  val cleanUsername = username.trim()
  val internalEmail = usernameToInternalEmail(cleanUsername)
  scope.launch {
    try {
      val result = Firebase.auth.createUserWithEmailAndPassword(internalEmail, password).await()
      runCatching {
        result.user?.updateProfile(
          userProfileChangeRequest {
            displayName = cleanUsername
          }
        )?.await()
      }
      onAuthSuccess()
    } catch (e: Exception) {
      Log.w(TAG, "Registration failed for $internalEmail", e)
      onAuthError(mapFirebaseAuthError(e, isRegistration = true))
    }
  }
}

fun signOutUser() {
  Firebase.auth.signOut()
}

private fun mapFirebaseAuthError(e: Exception, isRegistration: Boolean): String {
  val code = (e as? FirebaseAuthException)?.errorCode
  return when {
    e is FirebaseAuthUserCollisionException || code == "ERROR_EMAIL_ALREADY_IN_USE" ->
      "This username is already registered. Please log in or choose another."
    e is FirebaseAuthWeakPasswordException || code == "ERROR_WEAK_PASSWORD" ->
      "Password must be at least 6 characters."
    e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException ||
      code in setOf("ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND") ->
      "Wrong username or password."
    code == "ERROR_OPERATION_NOT_ALLOWED" ->
      "Email/Password authentication is not enabled on Firebase Console."
    e is FirebaseNetworkException ->
      "Network error. Check your internet connection and try again."
    else -> e.localizedMessage ?: if (isRegistration) "Registration failed." else "Login failed."
  }
}
