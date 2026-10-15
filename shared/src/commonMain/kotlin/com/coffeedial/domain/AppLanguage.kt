package com.coffeedial.domain

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    SPANISH("es"),
    ENGLISH("en");

    companion object {
        fun fromStored(stored: String?): AppLanguage =
            entries.firstOrNull {
                it.name.equals(stored, ignoreCase = true) ||
                    it.code.equals(stored, ignoreCase = true)
            } ?: SYSTEM
    }
}
