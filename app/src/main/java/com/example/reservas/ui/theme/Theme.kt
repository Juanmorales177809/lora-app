package com.example.reservas.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CoriotLightBlue,
    onPrimary = Color.White,

    primaryContainer = Color(0xFF173B6D),
    onPrimaryContainer = Color.White,

    secondary = CoriotMagenta,
    onSecondary = Color.White,

    secondaryContainer = Color(0xFF5A1A4A),
    onSecondaryContainer = Color.White,

    tertiary = CoriotLightBlue,
    onTertiary = Color.White,

    background = Color(0xFF0E1116),
    onBackground = Color(0xFFECEFF4),

    surface = Color(0xFF171B22),
    onSurface = Color(0xFFECEFF4),

    surfaceVariant = Color(0xFF222832),
    onSurfaceVariant = Color(0xFFC8CED8),

    outline = Color(0xFF7A8492),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),

    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = CoriotNavy,
    onPrimary = Color.White,

    primaryContainer = Color(0xFFE6EEF8),
    onPrimaryContainer = CoriotNavy,

    secondary = CoriotMagenta,
    onSecondary = Color.White,

    secondaryContainer = Color(0xFFF8E5F1),
    onSecondaryContainer = CoriotMagenta,

    tertiary = CoriotLightBlue,
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = CoriotDarkGray,

    surface = Color.White,
    onSurface = CoriotDarkGray,

    surfaceVariant = Color(0xFFF0F2F6),
    onSurfaceVariant = Color(0xFF6B7280),

    outline = Color(0xFFD1D5DB),

    error = Color(0xFFB3261E),
    onError = Color.White,

    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun ReservasTheme(
    darkTheme: Boolean = false,
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