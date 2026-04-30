package com.slocator.fleetdriver.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SLocatorDarkColors = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = TextPrimary,
    primaryContainer = BrandPurpleDim,
    onPrimaryContainer = TextPrimary,

    secondary = BrandEmerald,
    onSecondary = Obsidian,
    secondaryContainer = BrandEmeraldDim,
    onSecondaryContainer = TextPrimary,

    tertiary = BrandPurpleLight,
    onTertiary = TextPrimary,

    background = Obsidian,
    onBackground = TextPrimary,

    surface = ObsidianElevated,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,

    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline,

    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onError = TextPrimary
)

@Composable
fun SLocatorTheme(content: @Composable () -> Unit) {
    val colorScheme = SLocatorDarkColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                colorScheme.background.toArgb().also { window.statusBarColor = it }
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content
    )
}
