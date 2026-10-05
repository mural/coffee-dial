package com.coffeedial.admin

import com.coffeedial.auth.AuthProvider
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class AdminClientTest {
    private class Auth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(
            AuthState.LoggedIn(User("subject", provider = AuthProvider.GOOGLE))
        )
        override suspend fun syncCredential() = "credential"
        override suspend fun signInWithGoogle() {}
        override suspend fun signInWithApple() {}
        override suspend fun signOut() {
            state.value = AuthState.LoggedOut
        }
    }

    @Test fun requiresServerPermissionAndDoesNotSendEmailAsAuthority() = runTest {
        val client = AdminClient(Auth()) { url, headers ->
            assertEquals("https://auth-coffee.muralooo.win/api/admin/access", url)
            assertEquals(mapOf("Authorization" to "Bearer credential"), headers)
            """{"allowed":false}"""
        }
        assertFalse(client.allowed())
    }

    @Test fun discardsAdminResponseIfAccountChangesDuringRequest() = runTest {
        val auth = Auth()
        val client = AdminClient(auth) { _, _ ->
            auth.signOut()
            """{"allowed":true}"""
        }
        assertFailsWith<IllegalStateException> { client.allowed() }
    }

    @Test fun loggedOutCannotFetchData() = runTest {
        val auth = Auth()
        auth.signOut()
        val client = AdminClient(auth) { _, _ -> error("must not request") }
        assertFailsWith<IllegalStateException> { client.overview() }
    }
}
