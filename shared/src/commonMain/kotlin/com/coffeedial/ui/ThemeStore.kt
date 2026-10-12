package com.coffeedial.ui

import androidx.compose.runtime.Composable
import com.coffeedial.domain.ThemeMode

internal expect fun readThemeMode(): ThemeMode

internal expect fun writeThemeMode(mode: ThemeMode)

/** Platform chrome around the Compose canvas: system bars on Android, page background on web. */
@Composable
internal expect fun ThemeChrome(dark: Boolean)
