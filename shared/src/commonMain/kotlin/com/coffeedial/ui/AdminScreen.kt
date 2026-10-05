package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.admin.AdminClient
import com.coffeedial.admin.AdminDetail
import com.coffeedial.admin.AdminOverview
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException

@Composable
fun AdminScreen(auth: AuthRepository) {
    val identity by auth.state.collectAsState()
    val client = remember(auth) { AdminClient(auth) }
    var overview by remember(identity) { mutableStateOf<AdminOverview?>(null) }
    var detail by remember(identity) { mutableStateOf<AdminDetail?>(null) }
    var selected by remember(identity) { mutableStateOf<String?>(null) }
    var after by remember(identity) { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(identity) { mutableStateOf<String?>(null) }
    LaunchedEffect(identity, selected, after, refresh) {
        overview = null
        detail = null
        error = null
        if (identity !is AuthState.LoggedIn) return@LaunchedEffect
        busy = true
        try {
            if (selected ==
                null
            ) {
                overview = client.overview(after)
            } else {
                detail = client.detail(selected!!)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = when {
                failure.message.orEmpty().contains(
                    "403"
                ) -> "Esta cuenta no tiene permiso de administrador."

                failure.message.orEmpty().contains(
                    "401"
                ) -> "La sesión venció. Volvé a iniciar sesión."

                else -> "No pudimos consultar el panel. Reintentá; tus datos no se modificaron."
            }
        } finally {
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Admin", style = MaterialTheme.typography.headlineSmall)
        Text("Solo lectura · datos sincronizados en el servidor")
        if (identity !is AuthState.LoggedIn) {
            Text("Iniciá sesión con la cuenta administradora.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected !=
                    null
                ) {
                    TextButton(onClick = { selected = null }) { Text("Todas las cuentas") }
                }
                Button(onClick = { refresh++ }, enabled = !busy) { Text("Actualizar") }
            }
            if (busy) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            overview?.let { data ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("${data.accounts} cuentas · ${data.shots} shots")
                        Text("Últimos 7 días: ${data.shots7Days} shots")
                        Text(
                            "Puntuación media: ${if (data.shots == 0) "Sin datos" else (data.ratingSum.toDouble() / data.shots).pretty()}"
                        )
                        Text(
                            "Las cuentas anteriores aparecen después de su próxima sincronización. Los cambios sin subir no están incluidos.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        Text(
                            "Actividad · últimos 30 días (UTC)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (data.daily.isEmpty()) item { Text("Sin shots en este período.") }
                    items(data.daily) { Text("${it.label}: ${it.count} shots") }
                    item {
                        Text("Estilos más usados", style = MaterialTheme.typography.titleMedium)
                    }
                    items(data.styles) { Text("${it.label}: ${it.count}") }
                    item { Text("Cuentas", style = MaterialTheme.typography.titleMedium) }
                    items(data.accountsPage, key = { it.subject }) { account ->
                        Card(onClick = {
                            selected = account.subject
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(account.email, style = MaterialTheme.typography.titleMedium)
                                Text("${account.shots} shots")
                                Text("Última confirmación: ${timestamp(account.confirmedAt)}")
                            }
                        }
                    }
                    item {
                        FlowRow {
                            if (after.isNotEmpty()) {
                                TextButton(onClick = {
                                    after = ""
                                }) { Text("Primera página") }
                            }
                            data.next?.let { next ->
                                TextButton(onClick = { after = next }) { Text("Más cuentas") }
                            }
                        }
                    }
                }
            }
            detail?.let { AdminShotHistory(it) }
        }
    }
}

@Composable
private fun AdminShotHistory(data: AdminDetail) {
    var style by remember(data.account.subject) { mutableStateOf("") }
    var from by remember(data.account.subject) { mutableStateOf("") }
    var to by remember(data.account.subject) { mutableStateOf("") }
    var rating by remember(data.account.subject) { mutableStateOf("") }
    val fromTime = from.takeIf { it.isNotBlank() }?.let {
        runCatching { Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() }.getOrNull()
    }
    val toTime = to.takeIf { it.isNotBlank() }?.let {
        runCatching {
            Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() + 86400000
        }.getOrNull()
    }
    val invalid = (from.isNotBlank() && fromTime == null) || (to.isNotBlank() && toTime == null) ||
        (fromTime != null && toTime != null && fromTime >= toTime) ||
        (rating.isNotBlank() && rating.toIntOrNull() !in 1..5)
    val shots = remember(data, style, fromTime, toTime, rating, invalid) {
        if (invalid) {
            emptyList()
        } else {
            data.shots.filter {
                (style.isBlank() || it.style.orEmpty().contains(style, ignoreCase = true)) &&
                    (fromTime == null || it.createdAt >= fromTime) &&
                    (toTime == null || it.createdAt < toTime) &&
                    (rating.isBlank() || it.rating == rating.toIntOrNull())
            }.sortedByDescending { it.createdAt }
        }
    }
    val beans = remember(data) { data.beans.associateBy { it.id } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(data.account.email, style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(style, {
                style = it
            }, label = { Text("Filtrar estilo") }, singleLine = true)
            OutlinedTextField(from, {
                from = it
            }, label = { Text("Desde · AAAA-MM-DD (UTC)") }, singleLine = true)
            OutlinedTextField(to, {
                to = it
            }, label = { Text("Hasta · AAAA-MM-DD (UTC)") }, singleLine = true)
            OutlinedTextField(rating, {
                rating = it
            }, label = { Text("Puntuación · 1 a 5 (opcional)") }, singleLine = true)
            if (invalid) {
                Text(
                    "Revisá las fechas y la puntuación.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text("${shots.size} de ${data.shots.size} shots")
        }
        items(shots, key = { it.id }) { shot ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        beans[shot.beanId]?.name ?: "Café",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(timestamp(shot.createdAt))
                    Text(
                        "${shot.dose.pretty()} g → ${shot.output.pretty()} g · ${shot.seconds.pretty()} s"
                    )
                    Text("Estilo: ${shot.style ?: "Sin registrar"} · Molienda: ${shot.grind}")
                    shot.extraWater?.let { Text("Agua extra: ${it.pretty()} ml") }
                    shot.milk?.let { Text("Leche: ${it.pretty()} ml") }
                    shot.temperature?.let { Text("Temperatura: ${it.pretty()} °C") }
                    shot.machine?.let { Text("Máquina: $it") }
                    CoffeeRating(shot.rating)
                    if (shot.notes.isNotBlank()) Text(shot.notes)
                }
            }
        }
    }
}
