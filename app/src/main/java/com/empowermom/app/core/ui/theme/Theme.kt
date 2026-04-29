package com.empowermom.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 设计系统：纯黑白简约风，对应 HTML 版本配色 ──────────────────────────────

object EmpowerMomColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Gray100 = Color(0xFFF9F9F9)  // cardBg
    val Gray200 = Color(0xFFF5F5F5)  // hoverColor
    val Gray300 = Color(0xFFE5E5E5)  // borderColor
    val Gray500 = Color(0xFFCCCCCC)
    val Gray700 = Color(0xFF666666)  // secondary text
    val CrisisRed = Color(0xFFEF4444)
    val CrisisBg = Color(0xFFFEF2F2)
}

private val LightColorScheme = lightColorScheme(
    primary = EmpowerMomColors.Black,
    onPrimary = EmpowerMomColors.White,
    primaryContainer = EmpowerMomColors.Gray100,
    onPrimaryContainer = EmpowerMomColors.Black,
    secondary = EmpowerMomColors.Gray700,
    onSecondary = EmpowerMomColors.White,
    secondaryContainer = EmpowerMomColors.Gray200,
    onSecondaryContainer = EmpowerMomColors.Black,
    background = EmpowerMomColors.White,
    onBackground = EmpowerMomColors.Black,
    surface = EmpowerMomColors.White,
    onSurface = EmpowerMomColors.Black,
    surfaceVariant = EmpowerMomColors.Gray100,
    onSurfaceVariant = EmpowerMomColors.Gray700,
    outline = EmpowerMomColors.Gray300,
    outlineVariant = EmpowerMomColors.Gray200,
    error = EmpowerMomColors.CrisisRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = EmpowerMomColors.White,
    onPrimary = EmpowerMomColors.Black,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = EmpowerMomColors.White,
    secondary = Color(0xFF999999),
    onSecondary = EmpowerMomColors.Black,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = EmpowerMomColors.White,
    background = Color(0xFF0A0A0A),
    onBackground = EmpowerMomColors.White,
    surface = Color(0xFF111111),
    onSurface = EmpowerMomColors.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF999999),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF222222),
)

@Composable
fun EmpowerMomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EmpowerMomTypography,
        shapes = EmpowerMomShapes,
        content = content
    )
}
