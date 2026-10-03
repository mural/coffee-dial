package com.coffeedial.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AndroidAuthRepository(
    private val context: Context,
    private val googleServerClientId: String = ""
) : AuthRepository {
    // Reauthenticate through the provider after process death; legacy email-only sessions are not trusted.
    private val web = AndroidWebLogin(context.applicationContext)
    private var activity = WeakReference<Activity>(null)
    private var completing = false
    private var generation = 0
    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }
    fun detach(activity: Activity) {
        if (this.activity.get() === activity) this.activity.clear()
    }
    fun onResume() {
        if (web.expire()) fail("El intento de acceso venció. Volvé a intentarlo.")
    }
    private val mutableState = MutableStateFlow<AuthState>(
        if (web.pending && !web.expire()) AuthState.Authenticating else AuthState.LoggedOut
    )
    override val state = mutableState.asStateFlow()
    private val manager by lazy { CredentialManager.create(context) }

    init {
        context.getSharedPreferences("coffee_auth", Context.MODE_PRIVATE).edit().clear().apply()
    }

    override suspend fun signInWithGoogle(): Unit = withContext(Dispatchers.Main) {
        if (mutableState.value ==
            AuthState.Authenticating
        ) {
            return@withContext
        }
        if (!googleServerClientId.endsWith(".apps.googleusercontent.com")) {
            return@withContext fail(
                "Google todavía no está configurado para esta versión de Coffee Dial."
            )
        }
        val host = activity.get() ?: return@withContext
        val attemptGeneration = ++generation
        val playServices = runCatching {
            context.packageManager.getApplicationInfo("com.google.android.gms", 0).enabled
        }.getOrDefault(false)
        if (!playServices) return@withContext signInWithBrowser()
        mutableState.value = AuthState.Authenticating
        try {
            val existing = GetGoogleIdOption.Builder()
                .setServerClientId(googleServerClientId)
                .setFilterByAuthorizedAccounts(true)
                .build()
            val response = try {
                manager.getCredential(host, GetCredentialRequest(listOf(existing)))
            } catch (_: NoCredentialException) {
                // The explicit button flow also supports adding a Google account.
                val interactive = GetSignInWithGoogleOption.Builder(googleServerClientId).build()
                manager.getCredential(host, GetCredentialRequest(listOf(interactive)))
            }
            val credential = response.credential
            require(
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            )
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            // Received directly from Credential Manager, never from a link or user input.
            // A future server must independently verify the token before granting data access.
            val payload =
                String(Base64.decode(google.idToken.split('.')[1], Base64.URL_SAFE), Charsets.UTF_8)
            val subject = Json.parseToJsonElement(payload).jsonObject["sub"]?.jsonPrimitive?.content
            require(!subject.isNullOrBlank())
            val user =
                User(
                    subject,
                    google.id,
                    google.displayName,
                    google.profilePictureUri?.toString(),
                    AuthProvider.GOOGLE
                )
            if (generation == attemptGeneration) mutableState.value = AuthState.LoggedIn(user)
        } catch (e: GetCredentialCancellationException) {
            if (generation == attemptGeneration) mutableState.value = AuthState.LoggedOut
        } catch (e: CancellationException) {
            if (generation == attemptGeneration) mutableState.value = AuthState.LoggedOut
            throw e
        } catch (_: androidx.credentials.exceptions.GetCredentialProviderConfigurationException) {
            if (generation != attemptGeneration) return@withContext
            mutableState.value = AuthState.LoggedOut
            signInWithBrowser()
        } catch (_: androidx.credentials.exceptions.GetCredentialUnsupportedException) {
            if (generation != attemptGeneration) return@withContext
            mutableState.value = AuthState.LoggedOut
            signInWithBrowser()
        } catch (_: NoCredentialException) {
            if (generation != attemptGeneration) return@withContext
            mutableState.value = AuthState.LoggedOut
            signInWithBrowser()
        } catch (_: Exception) {
            if (generation != attemptGeneration) return@withContext
            fail(
                "No se pudo iniciar sesión con Google. Revisá la conexión y la configuración de acceso de la app."
            )
        }
    }

    override val supportsBrowserSignIn = true

    override suspend fun signInWithBrowser() {
        if (mutableState.value == AuthState.Authenticating) return
        val host = activity.get() ?: return
        generation++
        mutableState.value = AuthState.Authenticating
        try {
            web.start(host)
        } catch (_: Exception) {
            fail("No se pudo abrir el navegador. Instalá o habilitá uno e intentá nuevamente.")
        }
    }

    suspend fun handleCallback(uri: Uri) {
        if (uri.scheme != "coffeedial" || uri.host != "auth" || uri.path != "/google") return
        if (completing || !web.pending) return
        completing = true
        val callbackGeneration = generation
        try {
            val user = web.complete(uri)
            if (user != null) {
                mutableState.value = AuthState.LoggedIn(user)
            } else if (!web.pending) {
                mutableState.value = AuthState.LoggedOut
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (generation != callbackGeneration) return
            web.clear()
            fail("No se pudo completar el acceso web. Volvé a intentarlo.")
        } finally {
            completing = false
        }
    }

    override suspend fun signInWithApple(): Unit = fail(
        "Apple en Android estará disponible cuando configuremos el servicio de acceso web. Podés seguir usando la app sin cuenta."
    )

    private fun fail(message: String) {
        mutableState.value = AuthState.Error(message)
    }

    override suspend fun signOut() {
        generation++
        web.clear()
        mutableState.value = AuthState.LoggedOut
        try {
            manager.clearCredentialState(ClearCredentialStateRequest())
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (
            _: Exception
        ) { /* Local sign-out still succeeds when the provider is unavailable. */ }
    }
}
