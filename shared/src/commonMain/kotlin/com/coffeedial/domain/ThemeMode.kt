package com.coffeedial.domain

/** Local appearance. It stays on the device and is not part of backup or sync. */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemDark
    }

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
