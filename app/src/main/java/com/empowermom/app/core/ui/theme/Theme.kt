package com.empowermom.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object EmpowerMomColors {
    // 暖色主调
    val Cream        = Color(0xFFFDF6EE)  // 页面底色
    val WarmWhite    = Color(0xFFFFF9F4)  // 顶栏/卡片底色
    val Peach        = Color(0xFFF4A87C)  // 主色（按钮渐变起点）
    val PeachLight   = Color(0xFFFAD5B8)  // 边框、分隔线
    val PeachPale    = Color(0xFFFEF0E4)  // 浅色背景/标签底色
    val Rose         = Color(0xFFE8826A)  // 强调色（按钮渐变终点、激活态）
    val RoseDark     = Color(0xFFC4614A)  // 按下态
    val Amber        = Color(0xFFF2C078)  // AI 回应区点缀色
    val AmberPale    = Color(0xFFFDF1D6)  // AI 回应区背景

    // 文字
    val TextDark     = Color(0xFF3A2E28)  // 主文字
    val TextMid      = Color(0xFF7A6358)  // 次要文字
    val TextLight    = Color(0xFFB09B8E)  // 占位/辅助文字

    // 功能色（不变）
    val CrisisRed    = Color(0xFFEF4444)
    val CrisisBg     = Color(0xFFFFF5F5)

    // 深色模式（保守调整，保持暖调）
    val DarkBg       = Color(0xFF1C1410)
    val DarkSurface  = Color(0xFF261E18)
    val DarkSurfaceVariant = Color(0xFF2E2420)
    val DarkBorder   = Color(0xFF3D3028)
}

private val LightColorScheme = lightColorScheme(
    primary              = EmpowerMomColors.Rose,
    onPrimary            = Color.White,
    primaryContainer     = EmpowerMomColors.PeachPale,
    onPrimaryContainer   = EmpowerMomColors.RoseDark,
    secondary            = EmpowerMomColors.Peach,
    onSecondary          = Color.White,
    secondaryContainer   = EmpowerMomColors.PeachPale,
    onSecondaryContainer = EmpowerMomColors.TextDark,
    background           = EmpowerMomColors.Cream,
    onBackground         = EmpowerMomColors.TextDark,
    surface              = EmpowerMomColors.WarmWhite,
    onSurface            = EmpowerMomColors.TextDark,
    surfaceVariant       = EmpowerMomColors.AmberPale,   // AI 回应区背景
    onSurfaceVariant     = EmpowerMomColors.TextMid,
    outline              = EmpowerMomColors.PeachLight,
    outlineVariant       = EmpowerMomColors.PeachPale,
    error                = EmpowerMomColors.CrisisRed,
)

private val DarkColorScheme = darkColorScheme(
    primary              = EmpowerMomColors.Peach,
    onPrimary            = EmpowerMomColors.TextDark,
    primaryContainer     = EmpowerMomColors.DarkSurfaceVariant,
    onPrimaryContainer   = EmpowerMomColors.PeachLight,
    secondary            = EmpowerMomColors.Amber,
    onSecondary          = EmpowerMomColors.TextDark,
    secondaryContainer   = EmpowerMomColors.DarkSurfaceVariant,
    onSecondaryContainer = EmpowerMomColors.PeachLight,
    background           = EmpowerMomColors.DarkBg,
    onBackground         = Color(0xFFF5EDE6),
    surface              = EmpowerMomColors.DarkSurface,
    onSurface            = Color(0xFFF5EDE6),
    surfaceVariant       = EmpowerMomColors.DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFB09B8E),
    outline              = EmpowerMomColors.DarkBorder,
    outlineVariant       = EmpowerMomColors.DarkSurfaceVariant,
    error                = EmpowerMomColors.CrisisRed,
)

@Composable
fun EmpowerMomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = EmpowerMomTypography,
        shapes      = EmpowerMomShapes,
        content     = content
    )
}