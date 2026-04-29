package com.hrm.breeze.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BreezePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F3FF),
    onPrimaryContainer = Color(0xFF002D5C),
    secondary = Color(0xFF637381),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F3F4),
    onSecondaryContainer = Color(0xFF232F3A),
    error = ErrorLight,
    onError = OnErrorLight,
    background = Color(0xFFF5F7FA),
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFF1F3F4),
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = BreezePrimaryDark,
    onPrimary = Color(0xFF002D5C),
    primaryContainer = Color(0xFF003A70),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF002D5C),
    secondaryContainer = Color(0xFF2A313A),
    onSecondaryContainer = Color(0xFFDCE3E9),
    error = ErrorDark,
    onError = OnErrorDark,
    background = Color(0xFF0F1419),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF1A1F25),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2A313A),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF2A313A),
    outlineVariant = Color(0xFF373E48),
    scrim = Color.Black,
)

@Stable
object BreezeTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val extendedColors: BreezeExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBreezeExtendedColors.current
}

@Composable
fun BreezeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkBreezeExtendedColors else LightBreezeExtendedColors

    CompositionLocalProvider(
        LocalBreezeExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
