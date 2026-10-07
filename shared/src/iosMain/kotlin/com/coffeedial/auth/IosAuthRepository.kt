package com.coffeedial.auth

import com.coffeedial.sync.httpGetText
import com.coffeedial.sync.httpPostJson
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withLock
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

    var onSyncTokenCleared: (() -> Unit)? = null
    var onAuthenticationRequired: (() -> Unit)? = null
    private val credentialMutex = kotlinx.coroutines.sync.Mutex()
    private var validatedToken: String? = null
    internal var sessionPost: suspend (String, String, Map<String, String>) -> String =
        ::httpPostJson
    internal var sessionGet: suspend (String, Map<String, String>) -> String =
        ::httpGetText

    private fun sameUser(user: User): Boolean {
        val current = (state.value as? AuthState.LoggedIn)?.user
        return current?.id == user.id && current.provider == user.provider
    }

    private fun clearSyncToken() {
        syncToken = null
        validatedToken = null
        onSyncTokenCleared?.invoke()
    }

    private fun verifyIdentity(value: String, user: User): kotlinx.serialization.json.JsonObject {
        val body = Json.parseToJsonElement(value).jsonObject
        val provider = body["provider"]?.jsonPrimitive?.content ?: "google"
        check(
            body["id"]?.jsonPrimitive?.content == user.id &&
                provider == user.provider.name.lowercase()
        ) {
            "La sesión guardada pertenece a otra cuenta. Volvé a iniciar sesión."
        }
        return body
    }

    override suspend fun syncCredential(): String? = withContext(Dispatchers.Main) {
        credentialMutex.withLock {
            val user = (state.value as? AuthState.LoggedIn)?.user ?: return@withLock null
            val saved = syncToken
            if (saved != null) {
                if (validatedToken == saved) return@withLock saved
                try {
                    val profile = sessionGet(
                        "https://auth-coffee.muralooo.win/api/session",
                        mapOf("Authorization" to "Bearer $saved")
                    )
                    if (!sameUser(user)) return@withLock null
                    try {
                        verifyIdentity(profile, user)
                    } catch (_: IllegalStateException) {
                        clearSyncToken()
                        return@withLock null
                    }
                    validatedToken = saved
                    return@withLock saved
                } catch (error: com.coffeedial.sync.SyncHttpException) {
                    if (error.status != 401) throw error
                    if (!sameUser(user)) return@withLock null
                    clearSyncToken()
                }
            }
            val providerToken = when (user.provider) {
                AuthProvider.GOOGLE -> {
                    val refresh = refreshGoogleAction ?: return@withLock null
                    suspendCancellableCoroutine<String?> { continuation ->
                        refresh { token ->
                            if (continuation.isActive) {
                                continuation.resume(if (sameUser(user)) token else null)
                            }
                        }
                    } ?: throw IllegalStateException(
                        "No se pudo renovar Google. Revisá la conexión y probá de nuevo."
                    )
                }

                AuthProvider.APPLE -> appleIdToken ?: return@withLock null
            }
            val response = sessionPost(
                "https://auth-coffee.muralooo.win/api/session",
                "{}",
                mapOf("Authorization" to "Bearer $providerToken")
            )
            if (!sameUser(user)) return@withLock null
            val body = verifyIdentity(response, user)
            val token = requireNotNull(body["syncToken"]?.jsonPrimitive?.content)
            check(token.startsWith("cd."))
            syncToken = token
            validatedToken = token
            onSyncTokenObtained?.invoke(token)
            token
        }
    }

    override suspend fun refreshSyncCredential(rejected: String): String? {
        if (syncToken == rejected) clearSyncToken()
        return syncCredential()
    }

    override fun authenticationRequired(rejected: String?) {
        if (rejected != null && syncToken != null && syncToken != rejected) return
        clearSyncToken()
        appleIdToken = null
        onAuthenticationRequired?.invoke()
        failed("Volvé a continuar con tu proveedor para renovar el acceso. Tus datos se conservan.")
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
            clearSyncToken()
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
        if (!sameUser(user)) validatedToken = null
        mutableState.value = AuthState.LoggedIn(user)
    }
    fun failed(message: String) {
        mutableState.value = AuthState.Error(message)
    }
    fun cancelled() {
        googleIdToken = null
        appleIdToken = null
        clearSyncToken()
        mutableState.value = AuthState.LoggedOut
    }
    override suspend fun signOut() {
        val oldToken = syncToken
        googleIdToken = null
        appleIdToken = null
        clearSyncToken()
        signOutAction?.invoke()
        mutableState.value = AuthState.LoggedOut
        revokeSyncCredential(oldToken)
    }
}
