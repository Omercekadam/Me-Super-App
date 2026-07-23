package com.omer.mesuper.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Marka tohumu: Navy #14213D + Amber #FCA311. Tam M3 şeması bu ikiliden türetildi.
private val LightColors = lightColorScheme(
    primary = Color(0xFF14213D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2F5),
    onPrimaryContainer = Color(0xFF0A1526),
    secondary = Color(0xFFFCA311),
    onSecondary = Color(0xFF3A2A00),
    secondaryContainer = Color(0xFFFFE2B0),
    onSecondaryContainer = Color(0xFF2A1D00),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB8E6BA),
    onTertiaryContainer = Color(0xFF0A2E0C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEEF6),
    surfaceContainerHigh = Color(0xFFE7E8F0),
    surfaceContainerHighest = Color(0xFFE1E2EA),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF274777),
    onPrimaryContainer = Color(0xFFD7E2FF),
    secondary = Color(0xFFFFB951),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF613F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF7DD180),
    onTertiary = Color(0xFF003912),
    tertiaryContainer = Color(0xFF1F5423),
    onTertiaryContainer = Color(0xFF99EE9B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF1A1C21),
    surfaceContainer = Color(0xFF1E2025),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
)

@Composable
fun MeSuperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** false = marka paleti (varsayılan), true = sistem/Material You dinamik renk. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val moduleColors = if (darkTheme) DarkModuleColors else LightModuleColors

    CompositionLocalProvider(LocalModuleColors provides moduleColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
