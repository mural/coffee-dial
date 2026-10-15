@file:JvmName("AndroidLanguageStore")

package com.coffeedial.ui

import android.content.Context
import com.coffeedial.domain.AppLanguage
import java.util.Locale

private const val PREFS = "coffee_settings"
private const val LANGUAGE_KEY = "language"

private val androidContext: Context?
    get() = try {
        // Access existing context if installed via ThemeStore
        val field = Class.forName("com.coffeedial.ui.ThemeStoreKt")
            .getDeclaredField("themeContext")
        field.isAccessible = true
        field.get(null) as? Context
    } catch (_: Exception) {
        null
    }

internal actual fun readAppLanguage(): AppLanguage {
    val stored = androidContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(LANGUAGE_KEY, null)
    return AppLanguage.fromStored(stored)
}

internal actual fun writeAppLanguage(language: AppLanguage) {
    androidContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(LANGUAGE_KEY, language.name)
        ?.apply()
}

internal actual fun getPlatformLanguage(): AppLanguage {
    val lang = Locale.getDefault().language.lowercase()
    return if (lang.startsWith("en")) AppLanguage.ENGLISH else AppLanguage.SPANISH
}
