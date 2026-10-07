package com.coffeedial.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SyncHttpException(val status: Int, val code: String?) :
    Exception(
        when {
            status == 401 ->
                "Necesitamos renovar tu acceso. " +
                    "Entrá a Cuenta y continuá con tu proveedor."

            status == 403 -> "Esta cuenta no tiene acceso a la sincronización."

            status == 409 ->
                "Otro dispositivo guardó cambios. " +
                    "Volvé a sincronizar; tus datos se conservan."

            status == 426 || code == "backup_upgrade_required" ->
                "Actualizá esta app: la nube contiene datos de una versión más nueva."

            status == 413 ->
                "El historial supera el límite de sync. " +
                    "Exportá un backup para conservarlo."

            status == 422 -> "No se pudieron validar los datos ($code). Se conservan sin cambios."

            status == 429 ->
                "Hubo demasiados intentos. Esperá un momento y volvé a sincronizar."

            status >= 500 ->
                "El servicio no está disponible ahora. " +
                    "Reintentá; no hace falta cerrar sesión."

            else -> "No se pudo completar el sync ($status${code?.let { ": $it" }.orEmpty()})."
        }
    )

internal fun httpFailure(status: Int, body: String): SyncHttpException {
    val code = runCatching {
        Json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()?.takeIf { it.length <= 64 && it.all { c -> c.isLetterOrDigit() || c == '_' } }
    return SyncHttpException(status, code)
}
