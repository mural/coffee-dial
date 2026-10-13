package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.ShotTimer
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeOutlinedButton as OutlinedButton
import com.coffeedial.ui.CoffeeTextButton as TextButton
import kotlinx.coroutines.delay

@Composable
fun ShotTimerScreen(timer: ShotTimer, fromForm: Boolean, onUse: (String, String) -> Unit) {
    var frame by remember { mutableLongStateOf(0L) }
    val elapsed = remember(frame) { timer.elapsedMillis }
    LaunchedEffect(timer.running) {
        while (timer.running) {
            delay(50)
            if (timer.elapsedMillis >= ShotTimer.MAX_MILLIS) timer.pause()
            frame++
        }
    }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Shot Timer", style = MaterialTheme.typography.headlineLarge)
        Text("Iniciá al activar la extracción y marcá cuándo aparece la primera gota.")
        CoffeeSection("Extracción en curso") {
            Text(
                "${(elapsed / 60000).toString().padStart(2, '0')}:" +
                    "${(elapsed / 1000 % 60).toString().padStart(2, '0')}.${elapsed / 100 % 10}",
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Monospace
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = timer.running || elapsed < ShotTimer.MAX_MILLIS, onClick = {
                    if (timer.running) timer.pause() else timer.start()
                    frame++
                }) {
                    Text(
                        if (timer.running) {
                            "Pausar"
                        } else if (elapsed == 0L) {
                            "Iniciar"
                        } else {
                            "Continuar"
                        }
                    )
                }
                OutlinedButton(enabled = elapsed > 0 || timer.running, onClick = {
                    timer.reset()
                    frame++
                }) { Text("Reiniciar") }
            }
            OutlinedButton(enabled = timer.running && timer.firstDropMillis == null, onClick = {
                timer.markFirstDrop()
                frame++
            }) { Text("Marcar primera gota") }
            timer.firstDropMillis?.let {
                Text(
                    "Primera gota: ${(it / 1000.0).pretty()} s",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = {
                    timer.clearFirstDrop()
                    frame++
                }) { Text("Quitar marca") }
            }
            if (elapsed >= ShotTimer.MAX_MILLIS) Text("Se alcanzó el máximo de 10 minutos.")
        }

        CoffeeSection("Registrar la extracción") {
            Button(enabled = elapsed >= 100 && !timer.running, onClick = {
                onUse(
                    (elapsed / 1000.0).pretty(),
                    timer.firstDropMillis?.let {
                        (it / 1000.0).pretty()
                    }.orEmpty()
                )
            }) { Text(if (fromForm) "Usar estos tiempos" else "Registrar shot con estos tiempos") }
            Text("El total se mide desde el inicio e incluye el tiempo hasta la primera gota.")
        }
    }
}
