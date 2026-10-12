package com.coffeedial.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.coffeedial.domain.ThemeMode

private const val PREFS = "coffee_settings"
private const val THEME_KEY = "theme"

private var themeContext: Context? = null

fun installThemeStore(context: Context) {
    themeContext = context.applicationContext
}

internal actual fun readThemeMode(): ThemeMode {
    val stored = themeContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(THEME_KEY, null)
    return ThemeMode.fromStored(stored)
}

internal actual fun writeThemeMode(mode: ThemeMode) {
    themeContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(THEME_KEY, mode.name)
        ?.apply()
}

@Composable
internal actual fun ThemeChrome(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
