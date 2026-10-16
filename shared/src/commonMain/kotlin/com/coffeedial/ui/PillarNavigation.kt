package com.coffeedial.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.coffeedial.ui.CoffeeButton as Button
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val LocalNavigationSpace = staticCompositionLocalOf { 0.dp }

private val ProgressIcon = ImageVector.Builder("Progress", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(4f, 19f)
        lineTo(4f, 13f)
        moveTo(10f, 19f)
        lineTo(10f, 9f)
        moveTo(16f, 19f)
        lineTo(16f, 4f)
        moveTo(2f, 21f)
        lineTo(22f, 21f)
    }
}.build()
private val AccountIcon = ImageVector.Builder("Account", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(16f, 7f)
        curveTo(16f, 12f, 8f, 12f, 8f, 7f)
        curveTo(8f, 2f, 16f, 2f, 16f, 7f)
        close()
        moveTo(4f, 21f)
        curveTo(4f, 12f, 20f, 12f, 20f, 21f)
    }
}.build()

@Composable
fun PillarNavigation(
    selected: String,
    enabled: Boolean,
    translucent: Boolean = false,
    onSelect: (String) -> Unit
) {
    val strings = LocalStrings.current
    val shape = RoundedCornerShape(28.dp)
    // Web draws into one canvas, so it has no system blur behind the bar.
    val fill = if (translucent) {
        Brush.verticalGradient(
            0f to Color(0xC4526360),
            0.2f to Color(0xE0526360),
            0.46f to Color(0xEE2C4540),
            1f to Color(0xF2263C38)
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF526360),
                Color(0xFF263C38)
            )
        )
    }
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))
    )
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        NavigationBar(
            modifier = Modifier.shadow(3.dp, shape).clip(shape)
                .background(fill)
                .then(
                    if (translucent) {
                        Modifier.background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.26f),
                                0.18f to Color.White.copy(alpha = 0.07f),
                                0.42f to Color.Transparent
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .border(1.dp, rim, shape),
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0, 0, 0, 0),
            tonalElevation = 0.dp
        ) {
            listOf(
                Triple("home", strings.navBrew, CoffeeCup),
                Triple("track", strings.navShots, BeanIcon),
                Triple("improve", strings.navAnalysis, ProgressIcon),
                Triple("account", strings.navAccount, AccountIcon)
            ).forEach { (route, label, icon) ->
                val active = selected == route
                val interaction = remember(route) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        pressed -> 0.88f
                        active -> 1.12f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pillarIconScale"
                )
                NavigationBarItem(
                    selected = active,
                    onClick = { onSelect(route) },
                    enabled = enabled,
                    interactionSource = interaction,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF173C32),
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFFCEE8DC),
                        unselectedIconColor = Color(0xFFD7E3DE),
                        unselectedTextColor = Color(0xFFD7E3DE)
                    ),
                    icon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun PillarScreen(title: String, description: String, actions: List<Pair<String, () -> Unit>>) {
    val strings = LocalStrings.current
    Column(
        Modifier.verticalScroll(
            rememberScrollState()
        ).padding(bottom = LocalNavigationSpace.current),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(description)
        CoffeeSection(if (title == strings.navShots) strings.yourRecords else strings.exploreResults) {
            actions.forEachIndexed { index, (label, action) ->
                if (index == 0) {
                    Button(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) }
                } else {
                    CoffeeOutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PillarNavigationPreview() {
    MaterialTheme {
        PillarNavigation(
            selected = "home",
            enabled = true,
            onSelect = { }
        )
    }
}

@Preview(showBackground = true, name = "Web")
@Composable
private fun PillarNavigationWebPreview() {
    MaterialTheme {
        PillarNavigation(
            selected = "track",
            enabled = true,
            translucent = true,
            onSelect = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PillarScreenPreview() {
    MaterialTheme {
        PillarScreen(
            title = "Registrar",
            description = "Tu café, tu equipo y cada extracción.",
            actions = listOf(
                "Nuevo Shot" to {},
                "Granos" to {},
                "Máquinas y Molinos" to {},
                "Tazas" to {}
            )
        )
    }
}
