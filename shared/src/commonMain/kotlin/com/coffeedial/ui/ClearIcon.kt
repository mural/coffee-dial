package com.coffeedial.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val CloseMark = ImageVector.Builder("Clear", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }
}.build()

@Composable
internal fun ClearIcon(label: String) {
    val strings = LocalStrings.current
    Icon(CloseMark, contentDescription = strings.clearField(label))
}

private val EditAmountMark = ImageVector.Builder("EditAmount", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 17.25f)
        lineTo(3f, 21f)
        lineTo(6.75f, 21f)
        lineTo(17.81f, 9.94f)
        lineTo(14.06f, 6.19f)
        close()
        moveTo(15.47f, 4.78f)
        lineTo(19.22f, 8.53f)
        lineTo(21f, 6.75f)
        lineTo(17.25f, 3f)
        close()
    }
}.build()

@Composable
internal fun EditAmountIcon(label: String) {
    Icon(EditAmountMark, contentDescription = label)
}
