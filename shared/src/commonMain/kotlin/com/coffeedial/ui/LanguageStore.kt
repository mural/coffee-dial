package com.coffeedial.ui

import com.coffeedial.domain.AppLanguage

internal expect fun readAppLanguage(): AppLanguage

internal expect fun writeAppLanguage(language: AppLanguage)

internal expect fun getPlatformLanguage(): AppLanguage

internal fun AppLanguage.resolve(platform: AppLanguage = getPlatformLanguage()): AppLanguage =
    when (this) {
        AppLanguage.SYSTEM -> platform
        AppLanguage.SPANISH -> AppLanguage.SPANISH
        AppLanguage.ENGLISH -> AppLanguage.ENGLISH
    }
