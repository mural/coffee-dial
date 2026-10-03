package com.coffeedial.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// AuthenticationServices and GoogleSignIn are owned by the Swift host.
class IosAuthRepository : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    override val state = mutableState.asStateFlow()
    var googleAction: (() -> Unit)? = null
    var appleAction: (() -> Unit)? = null
    var signOutAction: (() -> Unit)? = null

    private fun start(action: (() -> Unit)?) {
        if (mutableState.value ==
            AuthState.Authenticating
        ) {
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
        mutableState.value = AuthState.LoggedOut
    }
    override suspend fun signOut() {
        signOutAction?.invoke()
        mutableState.value = AuthState.LoggedOut
    }
}
