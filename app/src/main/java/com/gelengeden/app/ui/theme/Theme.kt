package com.gelengeden.app.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Color(0xFF1A1C1E),
    surface = CardLight,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF42474E),
    error = ExpenseRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = PrimaryBlueDark,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003731),
    background = SurfaceDark,
    onBackground = Color(0xFFE2E2E6),
    surface = CardDark,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC2C7CF),
    error = ExpenseRedLight,
    onError = Color.Black
)

@Composable
fun GelengedenTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
