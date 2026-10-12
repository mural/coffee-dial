package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {
    @Test fun lightStaysLightAndDarkStaysDark() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemDark = true))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemDark = false))
        assertEquals(true, ThemeMode.DARK.isDark(systemDark = false))
        assertEquals(true, ThemeMode.DARK.isDark(systemDark = true))
    }

    @Test fun systemFollowsTheDeviceAndUnknownValuesFallBack() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemDark = false))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("sepia"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored("LIGHT"))
    }
}
