package com.coffeedial.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal fun coffeeLightColorScheme() = lightColorScheme(
    primary = Color(0xFF704A32),
    background = Color(0xFFFCF8F3),
    surface = Color(0xFFFCF8F3),
    secondaryContainer = Color(0xFFEDE0CF)
)

internal fun coffeeDarkColorScheme() = darkColorScheme(
    primary = Color(0xFFE4C4A8),
    onPrimary = Color(0xFF3F2616),
    primaryContainer = Color(0xFF704A32),
    onPrimaryContainer = Color(0xFFF6E6D6),
    secondary = Color(0xFFD7C6B6),
    onSecondary = Color(0xFF3A2C22),
    secondaryContainer = Color(0xFF3A2C22),
    onSecondaryContainer = Color(0xFFF6EFE6),
    tertiary = Color(0xFFCEE8DC),
    onTertiary = Color(0xFF173C32),
    background = Color(0xFF1C1612),
    onBackground = Color(0xFFF6EFE6),
    surface = Color(0xFF1C1612),
    onSurface = Color(0xFFF6EFE6),
    surfaceVariant = Color(0xFF2A221C),
    onSurfaceVariant = Color(0xFFD7C6B6),
    outline = Color(0xFFA8907C),
    outlineVariant = Color(0xFF5C4A3C),
    surfaceDim = Color(0xFF140F0C),
    surfaceBright = Color(0xFF3A2F27),
    surfaceContainerLowest = Color(0xFF120E0B),
    surfaceContainerLow = Color(0xFF221B16),
    surfaceContainer = Color(0xFF2A221C),
    surfaceContainerHigh = Color(0xFF332922),
    surfaceContainerHighest = Color(0xFF3A2F27)
)
