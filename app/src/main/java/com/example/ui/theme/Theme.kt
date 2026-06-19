package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  selectedColorHex: String = "#1E88E5",
  selectedFontName: String = "Sử dụng mặc định",
  content: @Composable () -> Unit,
) {
  val parsedColor = try {
      Color(android.graphics.Color.parseColor(selectedColorHex))
  } catch (e: Exception) {
      Color(0xFF1E88E5) // Fallback to blue
  }

  val colorScheme = if (darkTheme) {
      darkColorScheme(
          primary = parsedColor,
          onPrimary = Color.Black,
          primaryContainer = parsedColor.copy(alpha = 0.15f),
          onPrimaryContainer = Color.White,
          secondary = parsedColor.copy(alpha = 0.8f),
          onSecondary = Color.Black,
          background = Color(0xFF121212),
          onBackground = Color(0xFFE3E2E6),
          surface = Color(0xFF1E1E1E),
          onSurface = Color(0xFFE3E2E6),
          surfaceVariant = Color(0xFF2D2D2D),
          onSurfaceVariant = Color(0xFFC5C6D0)
      )
  } else {
      lightColorScheme(
          primary = parsedColor,
          onPrimary = Color.White,
          primaryContainer = parsedColor.copy(alpha = 0.1f),
          onPrimaryContainer = parsedColor,
          secondary = parsedColor.copy(alpha = 0.8f),
          onSecondary = Color.White,
          background = Color(0xFFF9F9FC),
          onBackground = Color(0xFF1B1B1F),
          surface = Color.White,
          onSurface = Color(0xFF1B1B1F),
          surfaceVariant = Color(0xFFF1F1F5),
          onSurfaceVariant = Color(0xFF45464F)
      )
  }

  val family = when (selectedFontName) {
    "Serif" -> FontFamily.Serif
    "SansSerif" -> FontFamily.SansSerif
    "Monospace" -> FontFamily.Monospace
    else -> FontFamily.Default
  }

  val customTypography = Typography(
      bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
      bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
      bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
      titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
      titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
      titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
      headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
      headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.15.sp),
      headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.2.sp),
      labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
      labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
      labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
  )

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      val statusBarColor = if (darkTheme) {
        android.graphics.Color.parseColor("#121212")
      } else {
        // Light color scheme gets a status bar matching background or container
        try {
          android.graphics.Color.parseColor("#F9F9FC")
        } catch (e: Exception) {
          android.graphics.Color.parseColor("#EDE7F6")
        }
      }
      val navigationBarColor = if (darkTheme) {
        android.graphics.Color.parseColor("#121212")
      } else {
        try {
          android.graphics.Color.parseColor("#F9F9FC")
        } catch (e: Exception) {
          android.graphics.Color.parseColor("#EDE7F6")
        }
      }

      window.statusBarColor = statusBarColor
      window.navigationBarColor = navigationBarColor

      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = customTypography, content = content)
}
