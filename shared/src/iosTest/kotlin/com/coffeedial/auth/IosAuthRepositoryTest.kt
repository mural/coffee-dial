package com.coffeedial.auth

import com.coffeedial.sync.SyncHttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    @Test fun expiredSessionRefreshesGoogleBeforeMinting() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = IosAuthRepository()
            repo.authenticated(User("subject", provider = AuthProvider.GOOGLE))
            repo.syncToken = "cd.old"
            repo.googleIdToken = "expired-google"
            var persisted: String? = null
            repo.onSyncTokenObtained = { persisted = it }
            repo.sessionGet = { _, _ -> throw SyncHttpException(401, "unauthorized") }
            repo.refreshGoogleAction = { done -> done("fresh-google") }
            repo.sessionPost = { _, _, headers ->
                assertEquals("Bearer fresh-google", headers["Authorization"])
                """{"id":"subject","provider":"google","syncToken":"cd.new"}"""
            }
            assertEquals("cd.new", repo.syncCredential())
            assertEquals("cd.new", persisted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun temporaryOutagePreservesSavedSession() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = IosAuthRepository()
            repo.authenticated(User("subject", provider = AuthProvider.APPLE))
            repo.syncToken = "cd.saved"
            repo.sessionGet = { _, _ -> throw SyncHttpException(503, "service_unavailable") }
            assertFailsWith<SyncHttpException> { repo.syncCredential() }
            assertEquals("cd.saved", repo.syncToken)
            assertIs<AuthState.LoggedIn>(repo.state.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun savedSessionForAnotherAccountIsNotUsed() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = IosAuthRepository()
            repo.authenticated(User("subject", provider = AuthProvider.APPLE))
            repo.syncToken = "cd.other"
            repo.sessionGet = { _, _ -> """{"id":"other","provider":"apple"}""" }
            assertEquals(null, repo.syncCredential())
            assertEquals(null, repo.syncToken)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
