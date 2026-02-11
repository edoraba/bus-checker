package com.redergo.buspullman.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette personalizzata — blu trasporti / verde GPS
private val Blue600 = Color(0xFF1E88E5)
private val Blue800 = Color(0xFF1565C0)
private val Blue100 = Color(0xFFBBDEFB)
private val Blue50 = Color(0xFFE3F2FD)
private val Green600 = Color(0xFF43A047)
private val Green100 = Color(0xFFC8E6C9)
private val Amber700 = Color(0xFFFFA000)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Green600,
    onTertiary = Color.White,
    background = Color(0xFF0F1318),
    onBackground = Color(0xFFE1E3E8),
    surface = Color(0xFF171C22),
    onSurface = Color(0xFFE1E3E8),
    surfaceVariant = Color(0xFF1E2530),
    onSurfaceVariant = Color(0xFFBCC4D0),
    outline = Color(0xFF5A6270),
    outlineVariant = Color(0xFF3A424E)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue800,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Green600,
    onSecondary = Color.White,
    secondaryContainer = Green100,
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Green600,
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFE0E2E8)
)

@Composable
fun BusPullmanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
