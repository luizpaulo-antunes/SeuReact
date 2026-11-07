package com.seureact.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SeuReactColors = darkColorScheme(
    primary = Color(0xFFFF5C7A),
    onPrimary = Color(0xFF30000E),
    primaryContainer = Color(0xFF4A1828),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFF6DE7D0),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0A4F45),
    onSecondaryContainer = Color(0xFF91F8E3),
    tertiary = Color(0xFFAEC6FF),
    onTertiary = Color(0xFF122F60),
    tertiaryContainer = Color(0xFF263F72),
    onTertiaryContainer = Color(0xFFD9E2FF),
    background = Color(0xFF090A0F),
    onBackground = Color(0xFFF5F0F3),
    surface = Color(0xFF111218),
    onSurface = Color(0xFFF5F0F3),
    surfaceVariant = Color(0xFF2D2B35),
    onSurfaceVariant = Color(0xFFCFC5CC),
    outline = Color(0xFF998D95),
    outlineVariant = Color(0xFF4B4248),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val SeuReactTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

private val SeuReactShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun SeuReactTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeuReactColors,
        typography = SeuReactTypography,
        shapes = SeuReactShapes,
        content = content,
    )
}
