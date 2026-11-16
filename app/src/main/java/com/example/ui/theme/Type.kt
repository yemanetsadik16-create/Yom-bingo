package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairDisplayFontFamily = FontFamily(
  Font(R.font.playfair_display, FontWeight.Normal),
  Font(R.font.playfair_display, FontWeight.SemiBold),
  Font(R.font.playfair_display, FontWeight.Bold),
)

val RobotoFontFamily = FontFamily(
  Font(R.font.roboto, FontWeight.Normal),
  Font(R.font.roboto, FontWeight.Medium),
  Font(R.font.roboto, FontWeight.Bold),
)

val YemeBingoTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = PlayfairDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 46.sp,
    lineHeight = 52.sp,
    letterSpacing = 0.sp,
  ),
  displaySmall = TextStyle(
    fontFamily = PlayfairDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = PlayfairDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = PlayfairDisplayFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    lineHeight = 28.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = RobotoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 24.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = RobotoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.2.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = RobotoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = RobotoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = RobotoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
)
