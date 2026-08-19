package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GoogleTealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = GoogleTealContainer,
    onPrimaryContainer = GoogleOnTealContainer,
    secondary = GoogleTealLight,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = GoogleWarningAmber,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoogleWarningContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = GoogleSurfaceBg,
    onBackground = GoogleTextPrimary,
    surface = GoogleSurfaceCard,
    onSurface = GoogleTextPrimary,
    surfaceVariant = GoogleSurfaceVariant,
    onSurfaceVariant = GoogleTextSecondary,
    outline = SlateOutline,
    outlineVariant = GoogleBorderLight,
    error = GoogleDangerRed,
    errorContainer = GoogleDangerContainer,
    onError = TealOnPrimary,
    onErrorContainer = GoogleDangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = DarkTealOnPrimary,
    primaryContainer = DarkTealPrimaryContainer,
    onPrimaryContainer = DarkTealOnPrimaryContainer,
    secondary = DarkTealPrimary,
    onSecondary = DarkTealOnPrimary,
    background = DarkSlateBackground,
    onBackground = DarkSlateOnBackground,
    surface = DarkSlateSurface,
    onSurface = DarkSlateOnSurface,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = DarkSlateOnSurfaceVariant
)

@Composable
fun SocietyTheme(
    darkTheme: Boolean = false, // Default to light theme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SocietySarvTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SocietyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
