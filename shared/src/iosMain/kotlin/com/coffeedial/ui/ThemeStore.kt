package com.coffeedial.ui

import androidx.compose.runtime.Composable
import com.coffeedial.domain.ThemeMode
import platform.Foundation.NSUserDefaults

private const val THEME_KEY = "coffee_theme"

internal actual fun readThemeMode(): ThemeMode =
    ThemeMode.fromStored(NSUserDefaults.standardUserDefaults.stringForKey(THEME_KEY))

internal actual fun writeThemeMode(mode: ThemeMode) {
    NSUserDefaults.standardUserDefaults.setObject(mode.name, forKey = THEME_KEY)
}

// SwiftUI reads "coffee_theme" and sets preferredColorScheme, which drives the status bar.
@Composable
internal actual fun ThemeChrome(dark: Boolean) = Unit
