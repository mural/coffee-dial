package com.coffeedial.backup

import com.coffeedial.domain.BrewDefault
import com.coffeedial.domain.fromTenPointScale
import com.coffeedial.domain.Recipe
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class BackupV1(
    val format: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val beans: List<BackupBeanV1>,
    val shots: List<BackupShotV1>,
    val machines: List<BackupMachineV1> = emptyList(),
    val cups: List<BackupCupV1> = emptyList(),
    val grinders: List<BackupGrinderV1> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val brewDefaults: List<BrewDefault> = emptyList()
)

@Serializable
data class BackupBeanV1(
    val id: String,
    val name: String,
    val roaster: String,
    val archived: Boolean = false,
    val photo: com.coffeedial.photos.BeanPhoto? = null,
    val roastDate: Long? = null,
    val origin: String? = null,
    val process: String? = null,
    val bagWeightGrams: Double? = null
)

@Serializable
data class BackupMachineV1(val id: String, val name: String, val type: String, val year: String)

@Serializable
data class BackupCupV1(
    val id: String,
    val name: String,
    val weight: Double? = null,
    val capacity: Double? = null
)

@Serializable
data class BackupGrinderV1(val id: String, val name: String, val type: String)

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
    val rating: Int,
    val extraWater: Double? = null,
    val style: String? = null,
    val cup: String? = null,
    val firstDrop: Double? = null,
    val taste: com.coffeedial.domain.TasteProfile? = null,
    val dialIn: com.coffeedial.domain.DialInContext? = null,
    val machineId: String? = null,
    val cupId: String? = null,
    val grinder: String? = null,
    val grinderId: String? = null
)

class BackupException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

object BackupFormat {
    const val MAX_BYTES = 10 * 1024 * 1024
    const val CURRENT_VERSION = 14
    const val FORMAT = "coffee-dial-backup"
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun create(
        beans: List<BackupBeanV1>,
        shots: List<BackupShotV1>,
        machines: List<BackupMachineV1> = emptyList(),
        cups: List<BackupCupV1> = emptyList(),
        grinders: List<BackupGrinderV1> = emptyList(),
        recipes: List<Recipe> = emptyList(),
        brewDefaults: List<BrewDefault> = emptyList()
    ): BackupV1 = BackupV1(
        FORMAT,
        CURRENT_VERSION,
        Clock.System.now().toString(),
        beans,
        shots,
        machines,
        cups,
        grinders, recipes, brewDefaults
    )

    fun encode(backup: BackupV1): String {
        validate(backup)
        requirePhotos(backup)
        return json.encodeToString(backup).also { checkSize(it) }
    }

    fun sanitize(backup: BackupV1): BackupV1 {
        val beanIds = backup.beans.map { it.id }.toSet()
        val missingBeanIds = backup.shots.map { it.beanId }.filter { it !in beanIds }.toSet()
        val fixedBeans = if (missingBeanIds.isEmpty()) {
            backup.beans
        } else {
            backup.beans +
                missingBeanIds.map { BackupBeanV1(id = it, name = "Café molido", roaster = "") }
        }
        return backup.copy(beans = fixedBeans)
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
            val rawBackup = when (version) {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 -> json.decodeFromJsonElement(
                    BackupV1.serializer(),
                    root
                )

                else -> throw BackupException("Esta versión de backup no es compatible ($version).")
            }
            validate(rawBackup)
            requirePhotos(rawBackup)
            return migrate(rawBackup)
        } catch (error: BackupException) {
            throw error
        } catch (_: SerializationException) {
            throw BackupException("El backup está dañado o contiene campos no compatibles.")
        } catch (e: IllegalArgumentException) {
            throw BackupException("El backup contiene datos inválidos: ${e.message ?: ""}")
        }
    }

    private fun requirePhotos(backup: BackupV1) {
        if (backup.beans.any { it.photo != null && it.photo.jpeg == null }) {
            throw BackupException(
                "El backup no incluye todas las fotos. Sincronizá antes de exportar."
            )
        }
    }

    private fun checkSize(text: String) {
        if (text.length > MAX_BYTES || text.encodeToByteArray().size > MAX_BYTES) {
            throw BackupException("El backup supera el límite de 10 MB.")
        }
    }

    fun migrate(backup: BackupV1): BackupV1 {
        validate(backup)
        val scaled = if (backup.schemaVersion < 14) {
            backup.copy(shots = backup.shots.map { it.copy(taste = it.taste?.fromTenPointScale()) })
        } else backup
        val linked = linkLegacyGrinders(linkLegacyEquipment(scaled)).copy(
            schemaVersion = CURRENT_VERSION
        )
        validate(linked)
        return linked
    }

    fun validate(backup: BackupV1) {
        fun valid(condition: Boolean) {
            if (!condition) throw BackupException("El backup contiene datos inválidos.")
        }
        valid(backup.format == FORMAT && backup.schemaVersion in 1..CURRENT_VERSION)
        valid(
            runCatching { Instant.parse(backup.exportedAt) }.isSuccess
        )
        valid(
            backup.beans.size <= 50_000 && backup.shots.size <= 50_000 &&
                backup.machines.size <= 50_000 && backup.cups.size <= 50_000 &&
                backup.grinders.size <= 50_000
        )
        val ids = backup.beans.map { it.id }.toSet()
        val machineIds = backup.machines.map { it.id }.toSet()
        val cupIds = backup.cups.map { it.id }.toSet()
        valid(backup.brewDefaults.isEmpty() || backup.schemaVersion >= 12)
        valid(backup.brewDefaults.size <= 4)
        valid(backup.brewDefaults.map { it.id }.distinct().size == backup.brewDefaults.size)
        valid(backup.brewDefaults.all { it.isValid() })
        valid(backup.recipes.isEmpty() || backup.schemaVersion >= 11)
        valid(backup.recipes.size <= 50_000)
        valid(backup.recipes.map { it.id }.toSet().size == backup.recipes.size)
        valid(backup.recipes.all { it.isValid() })
        val grinderIds = backup.grinders.map { it.id }.toSet()
        valid(ids.size == backup.beans.size)
        valid(backup.shots.map { it.id }.toSet().size == backup.shots.size)
        valid(backup.machines.map { it.id }.toSet().size == backup.machines.size)
        valid(backup.cups.map { it.id }.toSet().size == backup.cups.size)
        valid(backup.grinders.map { it.id }.toSet().size == backup.grinders.size)
        backup.beans.forEach {
            it.photo?.validate()
            valid(backup.schemaVersion >= 5 || it.photo == null)
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000 && it.roaster.length <= 10_000)
            valid(it.roastDate == null || (it.roastDate in 0..253402300799999L))
            valid(it.origin == null || it.origin.length <= 1000)
            valid(it.process == null || it.process.length <= 1000)
            valid(it.bagWeightGrams == null || (it.bagWeightGrams.isFinite() && it.bagWeightGrams > 0))
            valid(
                backup.schemaVersion >= 14 ||
                    (it.roastDate == null && it.origin == null && it.process == null && it.bagWeightGrams == null)
            )
        }
        backup.machines.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000)
        }
        backup.grinders.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000)
            valid(it.type.isNotBlank() && it.type.length <= 10_000)
        }
        backup.cups.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200)
            valid(it.name.isNotBlank() && it.name.length <= 10_000)
            valid(it.weight == null || (it.weight.isFinite() && it.weight > 0))
            valid(it.capacity == null || (it.capacity.isFinite() && it.capacity > 0))
            valid(backup.schemaVersion >= 13 || it.capacity == null)
        }
        backup.shots.forEach {
            valid(it.id.isNotBlank() && it.id.length <= 200 && it.beanId in ids)
            valid(it.createdAt >= 0 && it.createdAt <= 253402300799999L)
            valid(it.dose.isFinite() && it.dose > 0)
            valid(it.output.isFinite() && it.output > 0)
            valid(
                it.firstDrop == null ||
                    (
                        backup.schemaVersion >= 6 && it.firstDrop.isFinite() && it.firstDrop >= 0 &&
                            it.firstDrop <= it.seconds
                        )
            )
            valid(it.seconds.isFinite() && it.seconds > 0)
            valid(it.temperature == null || (it.temperature.isFinite() && it.temperature > 0))
            valid(it.milk == null || (it.milk.isFinite() && it.milk in 1.0..200.0))
            valid(
                it.extraWater == null || (it.extraWater.isFinite() && it.extraWater in 1.0..1000.0)
            )
            valid(it.style == null || it.style.length <= 100)
            valid(backup.schemaVersion >= 2 || (it.extraWater == null && it.style == null))
            valid(it.cup == null || it.cup.length <= 10_000)
            valid(it.machine == null || it.machine.length <= 10_000)
            valid(
                it.machineId == null ||
                    (
                        backup.schemaVersion >= 9 && it.machineId.length <= 200 &&
                            it.machineId in machineIds
                        )
            )
            valid(
                it.cupId == null ||
                    (backup.schemaVersion >= 9 && it.cupId.length <= 200 && it.cupId in cupIds)
            )
            valid(it.grinder == null || (backup.schemaVersion >= 10 && it.grinder.length <= 10_000))
            valid(
                it.grinderId == null ||
                    (
                        backup.schemaVersion >= 10 && it.grinderId.length <= 200 &&
                            it.grinderId in grinderIds
                        )
            )
            valid(it.grind.isNotBlank() && it.grind.length <= 10_000)
            valid(
                it.dialIn == null ||
                    (
                        backup.schemaVersion >= 8 && it.dialIn.isValid() &&
                            it.dialIn.previousShotId != it.id
                        )
            )
            valid(it.taste == null || (backup.schemaVersion >= 7 &&
                if (backup.schemaVersion >= 14) it.taste.isValid()
                else listOf(it.taste.body, it.taste.acidity, it.taste.bitterness).all { value -> value == null || value in 0..10 }))
            valid(it.notes.length <= 1_000_000 && it.rating in 1..5)
        }
    }
}
