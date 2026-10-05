package com.coffeedial.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class AndroidWebLogin(context: Context) {
    private val prefs = context.getSharedPreferences("coffee_web_attempt", Context.MODE_PRIVATE)
    private val origin = "https://auth-coffee.muralooo.win"
    var syncToken: String? = null
        private set
    val pending: Boolean get() = prefs.getString("state", null) != null

    fun clear() {
        check(prefs.edit().clear().commit())
    }

    fun expire(): Boolean {
        if (pending && (System.currentTimeMillis() - prefs.getLong("created", 0) !in 0..300_000)) {
            clear()
            return true
        }
        return false
    }

    fun start(context: Context) {
        syncToken = null
        val verifier = random()
        val state = random()
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )
        check(
            prefs.edit().putString("verifier", verifier).putString("state", state)
                .putLong("created", System.currentTimeMillis()).commit()
        )
        val url = Uri.parse("$origin/google/start").buildUpon()
            .appendQueryParameter(
                "challenge",
                challenge
            ).appendQueryParameter(
                "state",
                state
            ).appendQueryParameter("platform", "android").build()
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)
            )
        } catch (error: Exception) {
            clear()
            throw error
        }
    }

    suspend fun complete(uri: Uri): User? {
        require(uri.scheme == "coffeedial" && uri.host == "auth" && uri.path == "/google")
        if (!pending || expire()) return null
        val state = prefs.getString("state", null) ?: return null
        if (uri.getQueryParameters("state") != listOf(state)) return null
        val verifier = prefs.getString("verifier", null) ?: return null
        if (uri.getQueryParameter("error") != null) {
            clear()
            return null
        }
        val attempt = uri.getQueryParameter("attempt") ?: error("Missing attempt")
        val ticket = uri.getQueryParameter("code") ?: error("Missing code")
        require(
            attempt.matches(Regex("[A-Za-z0-9_-]{43}")) &&
                ticket.matches(Regex("[A-Za-z0-9_-]{43}"))
        )
        val body = buildJsonObject {
            put("attempt", attempt)
            put("ticket", ticket)
            put("verifier", verifier)
            put("appState", state)
        }.toString()
        val user = withContext(Dispatchers.IO) {
            val connection = URI("$origin/exchange").toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                check(connection.responseCode == 200)
                val text = connection.inputStream.use { stream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= 8192)
                        output.write(buffer, 0, count)
                    }
                    output.toString("UTF-8")
                }
                val json = Json.parseToJsonElement(text).jsonObject
                val id = json["id"]?.jsonPrimitive?.content ?: error("Missing subject")
                require(id.isNotBlank())
                syncToken = json["syncToken"]?.jsonPrimitive?.content
                User(
                    id,
                    json["email"]?.jsonPrimitive?.content,
                    json["displayName"]?.jsonPrimitive?.content?.takeUnless { it == "null" },
                    provider = AuthProvider.GOOGLE
                )
            } finally {
                connection.disconnect()
            }
        }
        // Cancel/sign-out or a newer attempt during network IO invalidates this result.
        if (prefs.getString("state", null) != state) return null
        clear()
        return user
    }

    private fun random(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        ByteArray(32).also { SecureRandom().nextBytes(it) }
    )
}
