package com.coffeedial.ui

import androidx.compose.runtime.Composable

// Shared header provides explicit navigation; browser URLs do not represent screens yet.
@Composable
internal actual fun PlatformBack(enabled: Boolean, onBack: () -> Unit) = Unit
