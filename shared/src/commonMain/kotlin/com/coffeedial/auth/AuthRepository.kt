package com.coffeedial.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    suspend fun syncCredential(): String? = null
    suspend fun refreshSyncCredential(rejected: String): String? = null
    fun authenticationRequired(rejected: String?) {}
    val isWebPlatform: Boolean get() = false
    val supportsBrowserSignIn: Boolean get() = false
    suspend fun signInWithBrowser() {}
    val state: StateFlow<AuthState>
    suspend fun signInWithGoogle(): Unit
    suspend fun signInWithApple(): Unit
    suspend fun signOut()
}

// Preview only: never manufactures an authenticated identity.
class InMemoryAuthRepository(initialState: AuthState = AuthState.LoggedOut) : AuthRepository {
    override val state = MutableStateFlow(initialState)
    override suspend fun signInWithGoogle(): Unit = Unit
    override suspend fun signInWithApple(): Unit = Unit
    override suspend fun signOut() {
        state.value = AuthState.LoggedOut
    }
}

internal suspend fun revokeSyncCredential(token: String?) {
    if (token?.startsWith("cd.") != true) return
    try {
        com.coffeedial.sync.httpPostJson(
            "https://auth-coffee.muralooo.win/api/logout",
            "{}",
            mapOf("Authorization" to "Bearer $token")
        )
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        // Local sign-out succeeds offline. The remote credential still has a bounded lifetime.
    }
}
