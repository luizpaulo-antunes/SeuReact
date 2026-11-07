package com.seureact.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFFFF5C7A),
    secondary = Color(0xFF72D7C5),
    background = Color(0xFF101114),
    surface = Color(0xFF1B1D22),
    surfaceVariant = Color(0xFF292C33),
)

@Composable
fun SeuReactTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
