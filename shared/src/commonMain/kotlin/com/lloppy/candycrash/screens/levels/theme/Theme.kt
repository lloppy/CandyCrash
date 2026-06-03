package com.lloppy.candycrash.screens.levels.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DeepSpace = Color(0xFF0D0B1A)
private val CosmicPurple = Color(0xFF1A0A2E)
private val NightSky = Color(0xFF121228)
private val SurfaceDark = Color(0xFF1E1E3F)
private val SurfaceCard = Color(0xFF252547)
private val MidnightPurple = Color(0xFF2D1B69)
private val Amethyst = Color(0xFF6B3FA0)
private val MoonWhite = Color(0xFFF5F5DC)
private val MoonSilver = Color(0xFFC0C0C0)
private val SubtleGray = Color(0xFFA0A0C0)
private val GoldAccent = Color(0xFFFFD700)
private val SoftGold = Color(0xFFDAA520)
private val StarPink = Color(0xFFE040FB)

private val LightColors = lightColorScheme()

private val CosmicColors = darkColorScheme(
    primary = Amethyst,
    onPrimary = MoonWhite,
    primaryContainer = MidnightPurple,
    onPrimaryContainer = MoonSilver,
    secondary = GoldAccent,
    onSecondary = DeepSpace,
    secondaryContainer = SoftGold,
    onSecondaryContainer = DeepSpace,
    tertiary = StarPink,
    onTertiary = DeepSpace,
    background = CosmicPurple,
    onBackground = MoonSilver,
    surface = NightSky,
    onSurface = MoonWhite,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = SubtleGray,
    surfaceContainerHigh = SurfaceCard,
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun GameTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) CosmicColors else LightColors,
            content = content,
        )
    }
}
