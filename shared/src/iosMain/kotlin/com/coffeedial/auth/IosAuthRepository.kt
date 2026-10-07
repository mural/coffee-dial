package com.coffeedial.auth

import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// AuthenticationServices and GoogleSignIn are owned by the Swift host.
class IosAuthRepository : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    override val state = mutableState.asStateFlow()
    var googleIdToken: String? = null
    var appleIdToken: String? = null
    var syncToken: String? = null
    var refreshGoogleAction: (((String?) -> Unit) -> Unit)? = null
    var onSyncTokenObtained: ((String) -> Unit)? = null

    private suspend fun obtainSyncToken(providerToken: String): String? {
        if (!syncToken.isNullOrBlank()) return syncToken
        return try {
            val response = com.coffeedial.sync.httpPostJson(
                "https://auth-coffee.muralooo.win/api/session",
                "{}",
                mapOf("Authorization" to "Bearer $providerToken")
            )
            val json = Json.parseToJsonElement(response).jsonObject
            val token = json["syncToken"]?.jsonPrimitive?.content
            if (!token.isNullOrBlank()) {
                syncToken = token
                onSyncTokenObtained?.invoke(token)
                token
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun syncCredential(): String? = withContext(Dispatchers.Main) {
        val user = (state.value as? AuthState.LoggedIn)?.user ?: return@withContext null
        if (!syncToken.isNullOrBlank()) return@withContext syncToken
        val providerToken = when (user.provider) {
            AuthProvider.GOOGLE -> {
                val refresh = refreshGoogleAction ?: return@withContext googleIdToken
                suspendCancellableCoroutine { continuation ->
                    refresh { token ->
                        if (continuation.isActive) {
                            val sameUser = (state.value as? AuthState.LoggedIn)?.user == user
                            continuation.resume(if (sameUser) token ?: googleIdToken else null)
                        }
                    }
                }
            }
            AuthProvider.APPLE -> appleIdToken
        } ?: return@withContext null
        val token = obtainSyncToken(providerToken)
        token ?: providerToken
    }

    var googleAction: (() -> Unit)? = null
    var appleAction: (() -> Unit)? = null
    var signOutAction: (() -> Unit)? = null

    private fun start(action: (() -> Unit)?) {
        if (mutableState.value == AuthState.Authenticating) {
            return
        }
        if (action == null) {
            failed("El acceso todavía no está disponible.")
        } else {
            mutableState.value = AuthState.Authenticating
            action()
        }
    }
    override suspend fun signInWithGoogle(): Unit = start(googleAction)
    override suspend fun signInWithApple(): Unit = start(appleAction)
    fun authenticated(user: User) {
        if (user.id.isBlank()) {
            failed("El proveedor no devolvió una identidad válida.")
            return
        }
        mutableState.value = AuthState.LoggedIn(user)
    }
    fun failed(message: String) {
        mutableState.value = AuthState.Error(message)
    }
    fun cancelled() {
        googleIdToken = null
        appleIdToken = null
        syncToken = null
        mutableState.value = AuthState.LoggedOut
    }
    override suspend fun signOut() {
        val oldToken = syncToken
        googleIdToken = null
        appleIdToken = null
        syncToken = null
        signOutAction?.invoke()
        mutableState.value = AuthState.LoggedOut
        revokeSyncCredential(oldToken)
    }
}
