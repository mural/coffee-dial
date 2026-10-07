@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.coffeedial.auth

import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private external object CoffeeAuth {
    fun start(): Promise<JsString>
    fun complete(): Promise<JsString>
    fun credential(): String
    fun rejected(token: String)
    fun clear()
}

class WasmAuthRepository : AuthRepository {
    override val isWebPlatform = true
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Authenticating)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()
    private var generation = 0
    private var syncToken: String? = null
    override suspend fun syncCredential(): String? = syncToken

    override suspend fun refreshSyncCredential(rejected: String): String? {
        val user = (state.value as? AuthState.LoggedIn)?.user ?: return null
        val saved = runCatching {
            Json.parseToJsonElement(CoffeeAuth.credential()).jsonObject
        }.getOrNull() ?: return null
        if (saved["id"]?.jsonPrimitive?.content != user.id) return null
        return saved["syncToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it != rejected }
            ?.also { syncToken = it }
    }

    override fun authenticationRequired(rejected: String?) {
        if (rejected != null && syncToken != null && syncToken != rejected) return
        syncToken?.let { CoffeeAuth.rejected(it) }
        syncToken = null
        mutableState.value = AuthState.Error(
            "Volvé a continuar con Google para renovar el acceso. Tus datos se conservan."
        )
    }

    init {
        val initialGeneration = generation
        MainScope().launch {
            try {
                val value = CoffeeAuth.complete().await().toString()
                if (generation == initialGeneration) {
                    mutableState.value = if (value.isEmpty()) {
                        AuthState.LoggedOut
                    } else {
                        val user = Json.parseToJsonElement(value).jsonObject
                        syncToken = user["syncToken"]?.jsonPrimitive?.contentOrNull
                        AuthState.LoggedIn(
                            User(
                                id = requireNotNull(user["id"]?.jsonPrimitive?.contentOrNull),
                                email = user["email"]?.jsonPrimitive?.contentOrNull,
                                displayName = user["displayName"]?.jsonPrimitive?.contentOrNull,
                                provider = AuthProvider.GOOGLE
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                if (generation == initialGeneration) {
                    mutableState.value =
                        AuthState.Error("No se pudo verificar el acceso. Volvé a iniciar sesión.")
                }
            }
        }
    }

    override suspend fun signInWithGoogle() {
        generation++
        mutableState.value = AuthState.Authenticating
        try {
            CoffeeAuth.start().await()
        } catch (_: Exception) {
            mutableState.value =
                AuthState.Error(
                    "No se pudo abrir Google. Habilitá el almacenamiento del navegador."
                )
        }
    }

    override suspend fun signInWithApple() {
        mutableState.value = AuthState.Error("Sign in con Apple no está configurado en Web")
    }

    override suspend fun signOut() {
        val oldToken = syncToken
        generation++
        syncToken = null
        CoffeeAuth.clear()
        mutableState.value = AuthState.LoggedOut
        revokeSyncCredential(oldToken)
    }
}
