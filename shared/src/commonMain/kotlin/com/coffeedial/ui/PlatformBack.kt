package com.coffeedial.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformBack(enabled: Boolean, onBack: () -> Unit)
