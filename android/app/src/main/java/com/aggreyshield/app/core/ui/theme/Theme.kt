package com.aggreyshield.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = ShieldPrimary,
    onPrimary = Color(0xFF00363D),
    secondary = ShieldSecondary,
    onSecondary = Color.White,
    background = ShieldBackgroundDark,
    onBackground = ShieldTextPrimaryDark,
    surface = ShieldSurfaceDark,
    onSurface = ShieldTextPrimaryDark,
    surfaceVariant = ShieldSurfaceVariantDark,
    onSurfaceVariant = ShieldTextSecondaryDark,
    outline = ShieldOutlineDark,
    error = SeverityCritical,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = ShieldPrimaryDark,
    onPrimary = Color.White,
    secondary = ShieldSecondary,
    onSecondary = Color.White,
    background = ShieldBackgroundLight,
    onBackground = ShieldTextPrimaryLight,
    surface = ShieldSurfaceLight,
    onSurface = ShieldTextPrimaryLight,
    surfaceVariant = ShieldSurfaceVariantLight,
    onSurfaceVariant = ShieldTextSecondaryLight,
    outline = ShieldOutlineLight,
    error = SeverityCritical,
    onError = Color.White,
)

/**
 * AGGREY SHIELD's Material 3 theme. [useDarkTheme] defaults to the platform setting when
 * [followSystem] is true, and to a forced dark cybersecurity look otherwise (the default,
 * since a security operations tool should look the same regardless of the phone's theme
 * unless the user explicitly asks to follow the system).
 */
@Composable
fun AggreyShieldTheme(
    forceDark: Boolean = true,
    followSystem: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = if (followSystem) isSystemInDarkTheme() else forceDark
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = AggreyShieldTypography,
        shapes = AggreyShieldShapes,
        content = content,
    )
}
