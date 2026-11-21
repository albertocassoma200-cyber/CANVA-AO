package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

class EmailPasswordProviderDisabledException(message: String) : IllegalStateException(message)

/** Returns the Firebase-generated Google OAuth client ID when google-services.json is configured. */
private fun getGoogleWebClientId(context: Context): String? {
    val resourceId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    return resourceId.takeIf { it != 0 }?.let(context::getString)
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private fun isOperationNotAllowedError(error: Exception): Boolean {
    if (error is FirebaseAuthException && error.errorCode == "ERROR_OPERATION_NOT_ALLOWED") return true
    val msg = error.message.orEmpty()
    return msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
        msg.contains("sign-in provider is disabled", ignoreCase = true)
}

private fun mapEmailAuthError(error: Exception): Exception {
    if (isOperationNotAllowedError(error)) {
        return EmailPasswordProviderDisabledException(
            "O método E-mail/Palavra-passe precisa de estar ativado na Consola Firebase (Authentication → Método de início de sessão → E-mail/Palavra-passe)."
        )
    }
    val message = when (error) {
        is FirebaseAuthWeakPasswordException -> "Palavra-passe fraca. Use pelo menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException -> "E-mail inválido ou palavra-passe incorreta."
        is FirebaseAuthInvalidUserException -> "Conta não encontrada para este e-mail."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta registada com este e-mail."
        else -> {
            if (error is FirebaseAuthException) {
                when (error.errorCode) {
                    "ERROR_TOO_MANY_REQUESTS" -> "Demasiadas tentativas. Aguarde alguns instantes."
                    "ERROR_USER_DISABLED" -> "Esta conta foi desativada."
                    else -> error.localizedMessage ?: "Falha na autenticação com e-mail."
                }
            } else {
                error.localizedMessage ?: "Falha na autenticação com e-mail."
            }
        }
    }
    return IllegalStateException(message)
}

suspend fun signInWithEmailPassword(
    email: String,
    password: String
): Result<FirebaseUser> {
    val cleanEmail = email.trim()
    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        return Result.failure(IllegalArgumentException("Introduza um e-mail válido (ex.: nome@gmail.com)."))
    }
    if (password.length < 6) {
        return Result.failure(IllegalArgumentException("A palavra-passe deve ter pelo menos 6 caracteres."))
    }
    return try {
        try {
            Firebase.auth.setLanguageCode("pt")
        } catch (_: Exception) {
        }
        val res = Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
        val user = res.user ?: return Result.failure(IllegalStateException("Sessão não iniciada."))
        Result.success(user)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (isOperationNotAllowedError(e)) {
            Log.w(TAG, "Email/Password provider is disabled in Firebase Console")
        } else {
            Log.w(TAG, "Email sign-in failed: ${e.message}")
        }
        Result.failure(mapEmailAuthError(e))
    }
}

suspend fun registerWithEmailPassword(
    displayName: String,
    email: String,
    password: String
): Result<FirebaseUser> {
    val cleanEmail = email.trim()
    val cleanName = displayName.trim()
    if (cleanName.isBlank()) {
        return Result.failure(IllegalArgumentException("Introduza o seu nome completo."))
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        return Result.failure(IllegalArgumentException("Introduza um e-mail Gmail válido."))
    }
    if (password.length < 6) {
        return Result.failure(IllegalArgumentException("A palavra-passe deve ter pelo menos 6 caracteres."))
    }
    return try {
        try {
            Firebase.auth.setLanguageCode("pt")
        } catch (_: Exception) {
        }
        val res = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val user = res.user ?: return Result.failure(IllegalStateException("Não foi possível criar a conta."))
        try {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName.take(80))
                    .build()
            ).await()
        } catch (_: Exception) {
        }
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send verification email immediately: ${e.message}")
        }
        Result.success(user)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (isOperationNotAllowedError(e)) {
            Log.w(TAG, "Email/Password provider is disabled in Firebase Console")
        } else {
            Log.w(TAG, "Email registration failed: ${e.message}")
        }
        Result.failure(mapEmailAuthError(e))
    }
}

suspend fun sendVerificationEmail(): Result<Unit> {
    val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("Nenhum utilizador autenticado."))
    return try {
        try {
            Firebase.auth.setLanguageCode("pt")
        } catch (_: Exception) {
        }
        user.sendEmailVerification().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "sendEmailVerification failed: ${e.message}")
        Result.failure(mapEmailAuthError(e))
    }
}

suspend fun reloadAndCheckEmailVerified(): Result<Boolean> {
    val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("Sessão expirada."))
    return try {
        user.reload().await()
        Result.success(Firebase.auth.currentUser?.isEmailVerified == true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "reload user failed: ${e.message}")
        Result.failure(mapEmailAuthError(e))
    }
}

suspend fun sendPasswordReset(email: String): Result<Unit> {
    val cleanEmail = email.trim()
    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        return Result.failure(IllegalArgumentException("Introduza um e-mail válido."))
    }
    return try {
        try {
            Firebase.auth.setLanguageCode("pt")
        } catch (_: Exception) {
        }
        Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "sendPasswordReset failed: ${e.message}")
        Result.failure(mapEmailAuthError(e))
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = getGoogleWebClientId(context)
    if (clientId.isNullOrBlank()) {
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
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
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
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = getGoogleWebClientId(context)
    if (clientId.isNullOrBlank()) {
        onAuthError("Configuração Google Sign-In ausente. Adicione o ficheiro google-services.json do projeto Firebase.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Contexto de Activity necessário para iniciar sessão.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Tipo de credencial inesperado.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Falha ao entrar com Google.")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
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
