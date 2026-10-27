package com.dias.shelfwise.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF244F40), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4ECD9), onPrimaryContainer = Color(0xFF203D2D),
    secondary = Color(0xFF756344), secondaryContainer = Color(0xFFF0E5CC),
    background = Color(0xFFF8F7F1), onBackground = Color(0xFF233B32),
    surface = Color(0xFFFFFEFA), onSurface = Color(0xFF233B32),
    surfaceVariant = Color(0xFFEDEFE4), onSurfaceVariant = Color(0xFF68746B),
    outline = Color(0xFF929D92), outlineVariant = Color(0xFFDFE4D7)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB2CFB6), onPrimary = Color(0xFF17392B),
    primaryContainer = Color(0xFF284F3B), onPrimaryContainer = Color(0xFFD8EAD4),
    background = Color(0xFF151D18), surface = Color(0xFF1A231C),
    onBackground = Color(0xFFE1E9DC), onSurface = Color(0xFFE1E9DC),
    surfaceVariant = Color(0xFF303B31), onSurfaceVariant = Color(0xFFB8C4B6)
)

@Composable
fun ShelfwiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 39.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 33.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
        ), content = content
    )
}
