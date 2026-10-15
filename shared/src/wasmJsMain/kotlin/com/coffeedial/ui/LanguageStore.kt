package com.coffeedial.ui

import com.coffeedial.domain.AppLanguage
import kotlinx.browser.localStorage
import kotlinx.browser.window

private const val LANGUAGE_KEY = "coffee_language"

internal actual fun readAppLanguage(): AppLanguage = try {
    AppLanguage.fromStored(localStorage.getItem(LANGUAGE_KEY))
} catch (_: Exception) {
    AppLanguage.SYSTEM
}

internal actual fun writeAppLanguage(language: AppLanguage) {
    try {
        localStorage.setItem(LANGUAGE_KEY, language.name)
    } catch (_: Exception) {
    }
}

internal actual fun getPlatformLanguage(): AppLanguage {
    val lang = try { window.navigator.language.lowercase() } catch (_: Exception) { "" }
    return if (lang.startsWith("en")) AppLanguage.ENGLISH else AppLanguage.SPANISH
}
