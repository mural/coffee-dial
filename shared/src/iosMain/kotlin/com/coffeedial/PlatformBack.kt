package com.coffeedial.ui

import androidx.compose.runtime.Composable

// The first iOS iteration uses the explicit back button in the shared header.
@Composable
internal actual fun PlatformBack(enabled: Boolean, onBack: () -> Unit) = Unit
