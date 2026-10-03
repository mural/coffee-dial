package com.coffeedial.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class IosAuthRepositoryTest {
    @Test
    fun missingHostCannotCreateSession() = runBlocking<Unit> {
        val repository = IosAuthRepository()
        repository.signInWithGoogle()
        assertIs<AuthState.Error>(repository.state.value)
    }

    @Test
    fun waitsForProviderAndIgnoresRepeatedTap() = runBlocking<Unit> {
        val repository = IosAuthRepository()
        var calls = 0
        repository.appleAction = { calls++ }
        repository.signInWithApple()
        repository.signInWithApple()
        assertEquals(1, calls)
        assertEquals(AuthState.Authenticating, repository.state.value)
        repository.authenticated(User(id = "apple-subject", provider = AuthProvider.APPLE))
        assertEquals(null, (repository.state.value as AuthState.LoggedIn).user.email)
    }

    @Test
    fun cancellationAndSignOutClearSession() = runBlocking<Unit> {
        val repository = IosAuthRepository()
        repository.googleAction = {}
        repository.signInWithGoogle()
        repository.cancelled()
        assertEquals(AuthState.LoggedOut, repository.state.value)
        repository.authenticated(User(id = "subject", provider = AuthProvider.GOOGLE))
        var signedOut = false
        repository.signOutAction = { signedOut = true }
        repository.signOut()
        assertEquals(true, signedOut)
        assertEquals(AuthState.LoggedOut, repository.state.value)
    }

    @Test
    fun emptyIdentityIsRejected() {
        val repository = IosAuthRepository()
        repository.authenticated(User(id = "", provider = AuthProvider.GOOGLE))
        assertIs<AuthState.Error>(repository.state.value)
    }
}
