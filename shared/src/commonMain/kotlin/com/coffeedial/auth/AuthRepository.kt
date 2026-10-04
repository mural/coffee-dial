package com.coffeedial.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
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
