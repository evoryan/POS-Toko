package com.example.ui.theme

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
    primary = PosBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PosBlueContainer,
    onPrimaryContainer = PosOnBlueContainer,
    secondary = PosTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = PosEmerald,
    onTertiary = Color.White,
    tertiaryContainer = PosEmeraldLight,
    onTertiaryContainer = Color(0xFF064E3B),
    background = PosBgLight,
    onBackground = PosTextPrimaryLight,
    surface = PosSurfaceLight,
    onSurface = PosTextPrimaryLight,
    surfaceVariant = PosSurfaceVariantLight,
    onSurfaceVariant = PosTextSecondaryLight,
    outline = PosOutlineLight,
    error = PosRose,
    onError = Color.White,
    errorContainer = PosRoseLight,
    onErrorContainer = Color(0xFF881337)
)

private val DarkColorScheme = darkColorScheme(
    primary = PosBlueLight,
    onPrimary = PosNavyDark,
    primaryContainer = PosBluePrimary,
    onPrimaryContainer = Color.White,
    secondary = PosTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = PosEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = PosEmeraldLight,
    background = PosBgDark,
    onBackground = PosTextPrimaryDark,
    surface = PosSurfaceDark,
    onSurface = PosTextPrimaryDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurfaceVariant = PosTextSecondaryDark,
    outline = PosOutlineDark,
    error = PosRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded POS colors consistent
    content: @Composable () -> Unit,
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
