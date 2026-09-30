package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightStudioColorScheme = lightColorScheme(
  primary = Color(0xFF007ACC),
  secondary = Color(0xFF0284C7),
  tertiary = Color(0xFFDC2626),
  background = Color(0xFF8C95A0),
  surface = Color.White,
  surfaceVariant = Color(0xFFF1F5F9),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = Color(0xFF1E293B),
  onSurface = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LightStudioColorScheme,
    typography = Typography,
    content = content
  )
}
