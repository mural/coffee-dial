package com.coffeedial.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.decimal
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShotRepository(driver: SqlDriver) {
    private val queries = CoffeeDatabase(driver).coffeeQueries

    val history: Flow<List<Shot>> = queries.history().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map {
            Shot(
                it.id, Bean(it.bean_id, it.name, it.roaster), it.created_at,
                it.dose, it.output, it.seconds, it.grind,
                it.temperature, it.notes, it.rating.toInt()
            )
        }
    }

    suspend fun save(draft: ShotDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El shot contiene valores inválidos" }
        queries.transaction {
            val name = draft.beanName.trim()
            val roaster = draft.roaster.trim()
            queries.insertBean(Uuid.random().toString(), name, roaster)
            val bean = queries.findBean(name, roaster).executeAsOne()
            queries.insertShot(
                Uuid.random().toString(), bean.id, Clock.System.now().toEpochMilliseconds(),
                requireNotNull(draft.dose.decimal()), requireNotNull(draft.output.decimal()),
                requireNotNull(
                    draft.seconds.decimal()
                ),
                draft.grind.trim(), draft.temperature.decimal(),
                draft.notes.trim(), draft.rating.toLong()
            )
        }
    }
}
