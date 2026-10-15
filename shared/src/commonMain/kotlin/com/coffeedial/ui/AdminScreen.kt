package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.coffeedial.ui.CoffeeButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
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
    val strings = LocalStrings.current
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
            val raw = failure.message.orEmpty()
            error = when {
                raw == "i18n.forbidden" || raw.contains("403") -> strings.noAdminPermission
                raw == "i18n.renew" || raw.contains("401") -> strings.sessionExpired
                raw.startsWith("i18n.") -> strings.userMessage(raw)
                else -> strings.adminQueryFailed
            }
        } finally {
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Admin", style = MaterialTheme.typography.headlineSmall)
        Text(strings.adminReadOnly)
        if (identity !is AuthState.LoggedIn) {
            Text(strings.adminSignIn)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected !=
                    null
                ) {
                    TextButton(onClick = { selected = null }) { Text(strings.allAccounts) }
                }
                Button(onClick = { refresh++ }, enabled = !busy) { Text(strings.refresh) }
            }
            if (busy) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            overview?.let { data ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(strings.accountsAndShots(data.accounts, data.shots))
                        Text(strings.last7Days(data.shots7Days))
                        Text(
                            strings.averageScore(
                                if (data.shots == 0) {
                                    strings.noScoreYet
                                } else {
                                    (data.ratingSum.toDouble() / data.shots).pretty()
                                }
                            )
                        )
                        Text(
                            strings.accountsAppearLater,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        Text(
                            strings.activity30Days,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (data.daily.isEmpty()) item { Text(strings.noShotsInPeriod) }
                    items(data.daily) {
                        Text("${it.label}: ${strings.shotCount(it.count)}")
                    }
                    item {
                        Text(strings.topStyles, style = MaterialTheme.typography.titleMedium)
                    }
                    items(data.styles) {
                        Text(strings.styleCount(strings.knownDrinkName(it.label), it.count))
                    }
                    item {
                        Text(strings.accountsHeading, style = MaterialTheme.typography.titleMedium)
                    }
                    items(data.accountsPage, key = { it.subject }) { account ->
                        Card(onClick = {
                            selected = account.subject
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(account.email, style = MaterialTheme.typography.titleMedium)
                                Text(strings.shotCount(account.shots))
                                Text(
                                    strings.lastConfirm(
                                        timestamp(account.confirmedAt, strings = strings)
                                    )
                                )
                            }
                        }
                    }
                    item {
                        FlowRow {
                            if (after.isNotEmpty()) {
                                TextButton(onClick = {
                                    after = ""
                                }) { Text(strings.firstPage) }
                            }
                            data.next?.let { next ->
                                TextButton(onClick = { after = next }) {
                                    Text(strings.moreAccounts)
                                }
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
    val strings = LocalStrings.current
    val beans = remember(data) { data.beans.associateBy { it.id } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(data.account.email, style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(style, {
                style = it
            }, label = { Text(strings.filterStyle) }, singleLine = true)
            OutlinedTextField(from, {
                from = it
            }, label = { Text(strings.fromDateUtc) }, singleLine = true)
            OutlinedTextField(to, {
                to = it
            }, label = { Text(strings.toDateUtc) }, singleLine = true)
            OutlinedTextField(rating, {
                rating = it
            }, label = { Text(strings.scoreOptional) }, singleLine = true)
            if (invalid) {
                Text(strings.checkDatesAndScore, color = MaterialTheme.colorScheme.error)
            }
            Text(strings.filteredShotCount(shots.size, data.shots.size))
        }
        items(shots, key = { it.id }) { shot ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        beans[shot.beanId]?.name ?: strings.beans,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(timestamp(shot.createdAt, strings = strings))
                    Text(
                        "${shot.dose.pretty()} g -> ${shot.output.pretty()} g · " +
                            "${shot.seconds.pretty()} s"
                    )
                    Text(
                        "${strings.styleLabel}: " +
                            "${shot.style?.let(strings::knownDrinkName) ?: strings.notRecorded}" +
                            " · ${strings.grind}: ${strings.grindLevelLabel(shot.grind)}"
                    )
                    shot.extraWater?.let { Text(strings.waterLine(it.pretty())) }
                    shot.milk?.let { Text(strings.milkLine(it.pretty())) }
                    shot.temperature?.let { Text(strings.temperatureLine(it.pretty())) }
                    shot.machine?.let { Text("${strings.machine}: $it") }
                    CoffeeRating(shot.rating)
                    if (shot.notes.isNotBlank()) Text(shot.notes)
                }
            }
        }
    }
}
