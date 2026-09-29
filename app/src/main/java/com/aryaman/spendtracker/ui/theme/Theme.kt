package com.aryaman.spendtracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ── Color Schemes ─────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = PrimaryIndigoDark,
    secondary = SecondaryTealDark,
    onSecondary = BackgroundDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = SecondaryTealDark,
    tertiary = TertiaryVioletDark,
    onTertiary = BackgroundDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = TertiaryVioletDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = RoseDebitDark,
    errorContainer = RoseContainerDark,
    onError = BackgroundDark,
    onErrorContainer = RoseDebitDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = PrimaryIndigo,
    secondary = SecondaryTeal,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = SecondaryTeal,
    tertiary = TertiaryViolet,
    onTertiary = SurfaceLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = TertiaryViolet,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = BackgroundLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = RoseDebit,
    errorContainer = RoseContainer,
    onError = SurfaceLight,
    onErrorContainer = RoseDebit
)

// ── Shape System ──────────────────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── Theme Composable ──────────────────────────────────────────
@Composable
fun SpendTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Color the system status bar to match the background
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
