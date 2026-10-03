package com.coffeedial.backup

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

// Wire DTOs are deliberately independent of database tables and editable domain models.
@Serializable
data class BackupV1(
    val format: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val beans: List<BackupBeanV1>,
    val shots: List<BackupShotV1>,
    val machines: List<BackupMachineV1> = emptyList()
)

@Serializable
data class BackupBeanV1(val id: String, val name: String, val roaster: String)

@Serializable
data class BackupMachineV1(val id: String, val name: String, val type: String, val year: String)

@Serializable
data class BackupShotV1(
    val id: String,
    val beanId: String,
    val createdAt: Long,
    val dose: Double,
    val output: Double,
    val seconds: Double,
    val grind: String,
    val temperature: Double?,
    val milk: Double? = null,
    val machine: String? = null,
    val notes: String,
    val rating: Int
)

class BackupException(message: String) : IllegalArgumentException(message)

object BackupFormat {
    const val MAX_BYTES = 10 * 1024 * 1024
    const val CURRENT_VERSION = 1
    const val FORMAT = "coffee-dial-backup"
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun create(
        beans: List<BackupBeanV1>,
        shots: List<BackupShotV1>,
        machines: List<BackupMachineV1> = emptyList()
    ): BackupV1 = BackupV1(
        FORMAT,
        CURRENT_VERSION,
        Clock.System.now().toString(),
        beans,
        shots,
        machines
    )

    fun encode(backup: BackupV1): String {
        validate(backup)
        return json.encodeToString(backup).also { checkSize(it) }
    }

    fun decode(text: String): BackupV1 {
        checkSize(text)
        try {
            val root = json.parseToJsonElement(text) as? JsonObject
                ?: throw BackupException("El archivo no es un backup de Coffee Dial.")
            val format = root["format"] as? JsonPrimitive
            if (format?.isString != true || format.content != FORMAT) {
                throw BackupException("El archivo no es un backup de Coffee Dial.")
            }
            val versionField = root["schemaVersion"] as? JsonPrimitive
            val version = versionField?.takeUnless { it.isString }?.intOrNull
                ?: throw BackupException("El backup no tiene una versión válida.")
            if (version > CURRENT_VERSION) {
                throw BackupException(
                    "Este backup es de una versión más nueva. Actualizá Coffee Dial."
                )
            }
            // Future formats get their own DTO and explicit vN -> vN+1 migration here.
            // Keep the v1 reader and its fixtures when adding v2. No guessed legacy format.
            val backup = when (version) {
                1 -> json.decodeFromJsonElement(BackupV1.serializer(), root)
                else -> throw BackupException("Esta versión de backup no es compatible ($version).")
            }
            validate(backup)
            return backup
        } catch (error: BackupException) {
            throw error
        } catch (_: SerializationException) {
            throw BackupException("El backup está dañado o contiene campos no compatibles.")
        } catch (_: IllegalArgumentException) {
            throw BackupException("El backup contiene datos inválidos.")
        }
    }

    private fun checkSize(text: String) {
        if (text.length > MAX_BYTES || text.encodeToByteArray().size > MAX_BYTES) {
            throw BackupException("El backup supera el límite de 10 MB.")
        }
    }

    fun validate(backup: BackupV1) {
        fun valid(condition: Boolean) {
            if (!condition) throw BackupException("El backup contiene datos inválidos.")
        }
        valid(backup.format == FORMAT && backup.schemaVersion == CURRENT_VERSION)
        valid(runCatching { Instant.parse(backup.exportedAt) }.isSuccess)
        valid(
            backup.beans.size <= 50_000 && backup.shots.size <= 50_000 &&
                backup.machines.size <= 50_000
        )
        val ids = backup.beans.map { it.id }.toSet()
        valid(ids.size == backup.beans.size)
        valid(backup.shots.map { it.id }.toSet().size == backup.shots.size)
        valid(backup.machines.map { it.id }.toSet().size == backup.machines.size)
        backup.beans.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000 && it.roaster.length <= 10_000)
        }
        backup.machines.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000)
        }
        backup.shots.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200 && it.beanId in ids)
            valid(it.createdAt >= 0 && it.createdAt <= 253402300799999L)
            valid(it.dose.isFinite() && it.dose > 0)
            valid(it.output.isFinite() && it.output > 0)
            valid(it.seconds.isFinite() && it.seconds > 0)
            valid(it.temperature == null || (it.temperature.isFinite() && it.temperature > 0))
            valid(it.milk == null || (it.milk.isFinite() && it.milk in 1.0..200.0))
            valid(it.machine == null || it.machine.length <= 10_000)
            valid(it.grind.isNotBlank() && it.grind.length <= 10_000)
            valid(it.notes.length <= 1_000_000 && it.rating in 1..5)
        }
    }
}
