package com.coffeedial.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.coffeedial.domain.ThemeMode
import kotlinx.browser.document
import kotlinx.browser.localStorage
import org.w3c.dom.HTMLElement

private const val THEME_KEY = "coffee_theme"

internal actual fun readThemeMode(): ThemeMode = try {
    ThemeMode.fromStored(localStorage.getItem(THEME_KEY))
} catch (_: Exception) {
    ThemeMode.SYSTEM
}

internal actual fun writeThemeMode(mode: ThemeMode) {
    try {
        localStorage.setItem(THEME_KEY, mode.name)
    } catch (_: Exception) {
        // Private mode can block storage. The choice still applies for this visit.
    }
}

@Composable
internal actual fun ThemeChrome(dark: Boolean) {
    SideEffect { applyPageChrome(dark) }
}

private fun applyPageChrome(dark: Boolean) {
    try {
        val root = document.documentElement as? HTMLElement ?: return
        root.style.setProperty("--page-bg", if (dark) "#1c1612" else "#fcf8f3")
        root.style.setProperty("--page-fg", if (dark) "#f6efe6" else "#342319")
        root.style.setProperty("--notice-bg", if (dark) "#3a2c22" else "#ede0cf")
        root.style.setProperty("color-scheme", if (dark) "dark" else "light")
        val theme = if (dark) "#1c1612" else "#704a32"
        document.querySelector("meta[name=\"theme-color\"]")
            ?.setAttribute("content", theme)
    } catch (_: Exception) {
    }
}
