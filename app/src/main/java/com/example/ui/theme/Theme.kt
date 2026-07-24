package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalColorScheme = darkColorScheme(
    primary = ForestSagePrimary,
    onPrimary = Color.White,
    primaryContainer = ForestSageContainer,
    onPrimaryContainer = ForestSageLight,
    secondary = HunterAmber,
    onSecondary = Color.Black,
    secondaryContainer = HunterAmberContainer,
    onSecondaryContainer = HunterAmber,
    tertiary = TopoCyan,
    background = TacticalDarkBackground,
    onBackground = TextPrimaryLight,
    surface = TacticalDarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun HuntAlignTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HuntAlignTheme(content = content)
}
