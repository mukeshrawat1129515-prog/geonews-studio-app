package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GeoDarkColorScheme = darkColorScheme(
    primary = GeoPrimary,
    onPrimary = GeoDarkBg,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoTextPrimary,
    secondary = GeoSecondary,
    onSecondary = GeoDarkBg,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoTextPrimary,
    tertiary = GeoGoldGlow,
    onTertiary = GeoDarkBg,
    background = GeoDarkBg,
    onBackground = GeoTextPrimary,
    surface = GeoDarkSurface,
    onSurface = GeoTextPrimary,
    surfaceVariant = GeoDarkSurfaceVariant,
    onSurfaceVariant = GeoTextSecondary,
    outline = GeoCardBorder,
    outlineVariant = GeoGoldBorder
)

private val GeoLightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = GeoDarkBg,
    secondary = GeoSecondary,
    onSecondary = GeoDarkBg,
    background = GeoDarkBg, // Keep consistent royal palace luxury dark aesthetic
    surface = GeoDarkSurface,
    onSurface = GeoTextPrimary,
    onBackground = GeoTextPrimary,
    outline = GeoCardBorder
)

@Composable
fun GeoNewsStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GeoDarkColorScheme else GeoLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
