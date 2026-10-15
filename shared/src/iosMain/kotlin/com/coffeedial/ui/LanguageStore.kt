package com.coffeedial.ui

import com.coffeedial.domain.AppLanguage
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val LANGUAGE_KEY = "coffee_language"

internal actual fun readAppLanguage(): AppLanguage =
    AppLanguage.fromStored(NSUserDefaults.standardUserDefaults.stringForKey(LANGUAGE_KEY))

internal actual fun writeAppLanguage(language: AppLanguage) {
    NSUserDefaults.standardUserDefaults.setObject(language.name, forKey = LANGUAGE_KEY)
}

internal actual fun getPlatformLanguage(): AppLanguage {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String ?: ""
    return if (preferred.lowercase().startsWith("en")) AppLanguage.ENGLISH else AppLanguage.SPANISH
}
