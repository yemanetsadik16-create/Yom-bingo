package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val YemeDarkColorScheme = darkColorScheme(
  primary = GoldPrimary,
  onPrimary = InkOnGold,
  primaryContainer = VioletDeep,
  onPrimaryContainer = GoldBright,
  secondary = VioletPrimary,
  onSecondary = Color.White,
  secondaryContainer = RoyalSurfaceElevated,
  onSecondaryContainer = InkLight,
  tertiary = RoomJoinOrange,
  onTertiary = Color.White,
  background = RoyalBgTop,
  onBackground = InkLight,
  surface = RoyalSurface,
  onSurface = InkLight,
  surfaceVariant = RoyalSurfaceElevated,
  onSurfaceVariant = InkMuted,
  outline = VioletOutline,
  outlineVariant = VioletLine,
  error = RoseError,
  onError = Color(0xFF3A0810),
)

val YemeShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun YemeBingoTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = YemeDarkColorScheme,
    typography = YemeBingoTypography,
    shapes = YemeShapes,
    content = content,
  )
}
