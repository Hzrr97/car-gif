package com.gifapp.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 车载深色高对比主题：大字号、大触控目标、低眩光
private val CarColors = darkColorScheme(
    primary = Color(0xFF4F8CFF),
    onPrimary = Color(0xFF001233),
    primaryContainer = Color(0xFF1E3A6E),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF7FD1C0),
    onSecondary = Color(0xFF00332B),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF151A23),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF232836),
    onSurfaceVariant = Color(0xFF9AA3B5),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0000),
    outline = Color(0xFF313848)
)

private val CarTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun GifCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CarColors,
        typography = CarTypography,
        content = content
    )
}