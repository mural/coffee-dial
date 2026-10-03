package com.coffeedial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBack(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}
