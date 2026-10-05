package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShotCard(shot: Shot, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shot.bean.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${shot.dose.pretty()} g -> ${shot.output.pretty()} g · ${shot.seconds.pretty()} s"
            )
            Text("1:${shot.ratio.pretty()} · ${timestamp(shot.createdAt)}")
            CoffeeRating(shot.rating)
        }
    }
}

@Preview(name = "Tarjeta de Shot", showBackground = true)
@Composable
private fun ShotCardPreview() {
    MaterialTheme {
        ShotCard(
            shot = mockShot,
            onClick = {}
        )
    }
}
