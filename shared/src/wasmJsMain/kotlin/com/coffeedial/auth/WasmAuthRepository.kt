package com.coffeedial.auth

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmAuthRepository : AuthRepository {
    override val isWebPlatform = true
    override val supportsBrowserSignIn = false

    private val mutableState = MutableStateFlow<AuthState>(readSavedWasmUser())
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    private fun readSavedWasmUser(): AuthState {
        try {
            val href = window.location.href
            if (href.contains("email=")) {
                val rawEmail = href.substringAfter("email=").substringBefore('&')
                val email = decodeURIComponent(rawEmail).trim()
                if (email.isNotBlank() && isValidEmail(email)) {
                    val rawName = if (href.contains("name=")) href.substringAfter("name=").substringBefore('&') else ""
                    val name = decodeURIComponent(rawName).trim().ifBlank { email.substringBefore('@') }
                    val user = User(id = email, email = email, displayName = name, provider = AuthProvider.GOOGLE)
                    saveWasmUser(user)
                    return AuthState.LoggedIn(user)
                }
            }

            val savedEmail = window.localStorage.getItem("coffee_user_email")
            if (!savedEmail.isNullOrBlank()) {
                val name = window.localStorage.getItem("coffee_user_name") ?: savedEmail.substringBefore('@')
                return AuthState.LoggedIn(
                    User(id = savedEmail, email = savedEmail, displayName = name, provider = AuthProvider.GOOGLE)
                )
            }
        } catch (_: Exception) {
        }
        return AuthState.LoggedOut
    }

    private fun decodeURIComponent(value: String): String {
        return value
            .replace("%40", "@")
            .replace("%40".lowercase(), "@")
            .replace("%20", " ")
            .replace("+", " ")
            .replace("%2B", "+")
    }

    private fun saveWasmUser(user: User) {
        try {
            window.localStorage.setItem("coffee_user_email", user.email ?: user.id)
            window.localStorage.setItem("coffee_user_name", user.displayName ?: "")
        } catch (_: Exception) {
        }
        mutableState.value = AuthState.LoggedIn(user)
    }

    override suspend fun signInWithGoogle() {
        mutableState.value = AuthState.Authenticating
        val challenge = "0123456789012345678901234567890123456789012"
        val appState = "0123456789012345678901234567890123456789012"
        val googleAuthUrl = "https://auth-coffee.muralooo.win/google/start?challenge=$challenge&state=$appState&platform=web"
        try {
            window.location.href = googleAuthUrl
        } catch (_: Exception) {
            mutableState.value = AuthState.Error("No se pudo redirigir al inicio de sesión de Google")
        }
    }

    override suspend fun signInWithApple() {
        mutableState.value = AuthState.Error("Sign in con Apple no está configurado en Web")
    }

    override suspend fun signOut() {
        try {
            window.localStorage.removeItem("coffee_user_email")
            window.localStorage.removeItem("coffee_user_name")
        } catch (_: Exception) {
        }
        mutableState.value = AuthState.LoggedOut
    }
}
