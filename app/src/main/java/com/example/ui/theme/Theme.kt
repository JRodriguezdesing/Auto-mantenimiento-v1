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

private val DarkColorScheme =
  darkColorScheme(
    primary = AutoAmber,
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = AutoAmberLight,
    secondary = AutoBlueLight,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = AutoGreen,
    onTertiary = Color(0xFF022C22),
    background = AutoNavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = AutoSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AutoSurfaceVariantDark,
    onSurfaceVariant = AutoSteelLight,
    error = AutoRed,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AutoPrimaryLight,
    onPrimary = AutoOnPrimaryLight,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AutoSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = AutoGreen,
    onTertiary = Color.White,
    background = AutoBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = AutoSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AutoSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF334155),
    error = AutoRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted automotive branding by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
