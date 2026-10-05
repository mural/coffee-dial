package com.coffeedial.admin

import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.sync.httpGetText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdminAccess(val allowed: Boolean)

@Serializable
data class AdminAccount(
    val subject: String,
    val email: String,
    val revision: Long,
    val confirmedAt: Long,
    val shots: Int,
    val ratingSum: Long
)

@Serializable
data class AdminCount(val label: String, val count: Int)

@Serializable
data class AdminOverview(
    val accounts: Int,
    val shots: Int,
    val ratingSum: Long,
    val shots7Days: Int,
    val accountsPage: List<AdminAccount>,
    val next: String? = null,
    val daily: List<AdminCount>,
    val styles: List<AdminCount>
)

@Serializable
data class AdminDetail(
    val account: AdminAccount,
    val revision: Long,
    val beans: List<BackupBeanV1>,
    val shots: List<BackupShotV1>
)

class AdminClient(
    private val auth: AuthRepository,
    private val get: suspend (String, Map<String, String>) -> String = ::httpGetText
) {
    private suspend fun request(path: String): String {
        val identity = auth.state.value as? AuthState.LoggedIn ?: error("Iniciá sesión con Google.")
        val token = auth.syncCredential() ?: error("Volvé a iniciar sesión con Google.")
        val result = get(
            "https://auth-coffee.muralooo.win/api/admin/$path",
            mapOf("Authorization" to "Bearer $token")
        )
        check(auth.state.value == identity) { "La cuenta cambió. Volvé a abrir Admin." }
        return result
    }
    suspend fun allowed(): Boolean = Json.decodeFromString<AdminAccess>(request("access")).allowed
    suspend fun overview(after: String = ""): AdminOverview =
        Json.decodeFromString(request("overview?after=${queryValue(after)}"))
    suspend fun detail(subject: String): AdminDetail =
        Json.decodeFromString(request("account?subject=${queryValue(subject)}"))
}

private fun queryValue(value: String): String = value.encodeToByteArray().joinToString("") {
    val n = it.toInt() and 255
    if (n in 65..90 || n in 97..122 || n in 48..57 || n in listOf(45, 46, 95, 126)) {
        n.toChar().toString()
    } else {
        "%" + n.toString(16).padStart(2, '0')
    }
}
