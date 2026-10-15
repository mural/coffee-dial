package com.coffeedial.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SyncHttpException(val status: Int, val code: String?) :
    Exception(
        when {
            status == 401 -> "i18n.renew"
            status == 403 -> "i18n.forbidden"
            status == 409 -> "i18n.stale"
            status == 426 || code == "backup_upgrade_required" -> "i18n.upgrade_app"
            status == 413 -> "i18n.too_big"
            status == 422 -> "i18n.invalid:${code.orEmpty()}"
            status == 429 -> "i18n.rate"
            status >= 500 -> "i18n.unavailable"
            else -> "i18n.sync_status:$status${code?.let { ":$it" }.orEmpty()}"
        }
    )

internal fun httpFailure(status: Int, body: String): SyncHttpException {
    val code = runCatching {
        Json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()?.takeIf { it.length <= 64 && it.all { c -> c.isLetterOrDigit() || c == '_' } }
    return SyncHttpException(status, code)
}
