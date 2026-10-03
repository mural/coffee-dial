package com.coffeedial.auth

enum class AuthProvider {
    GOOGLE,
    APPLE
}

data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val provider: AuthProvider,
    val linkedAt: Long = 0L
)

sealed interface AuthState {
    data object LoggedOut : AuthState
    data object Authenticating : AuthState
    data class LoggedIn(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}
