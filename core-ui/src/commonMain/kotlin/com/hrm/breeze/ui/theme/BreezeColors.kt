package com.hrm.breeze.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand colors
val BreezePrimary = Color(0xFF4A90E2)
val BreezePrimaryDark = Color(0xFF63A4FF)

// Semantic colors
val SuccessLight = Color(0xFF00B42A)
val OnSuccessLight = Color(0xFFFFFFFF)
val WarningLight = Color(0xFFFF9500)
val OnWarningLight = Color(0xFFFFFFFF)
val ErrorLight = Color(0xFFD93025)
val OnErrorLight = Color(0xFFFFFFFF)
val InfoLight = BreezePrimary
val OnInfoLight = Color(0xFFFFFFFF)

val SuccessDark = Color(0xFF34D399)
val OnSuccessDark = Color(0xFF002D18)
val WarningDark = Color(0xFFFBBF24)
val OnWarningDark = Color(0xFF2D1A00)
val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF420000)
val InfoDark = BreezePrimaryDark
val OnInfoDark = Color(0xFF002D5C)

// Text hierarchy
val TextPrimaryLight = Color(0xFF121212)
val TextSecondaryLight = Color(0xFF616161)
val TextTertiaryLight = Color(0xFF9E9E9E)
val TextDisabledLight = Color(0xFFC4C4C4)

val TextPrimaryDark = Color(0xFFF5F7FA)
val TextSecondaryDark = Color(0xFFE0E0E0)
val TextTertiaryDark = Color(0xFF9E9E9E)
val TextDisabledDark = Color(0xFF616161)

@Immutable
data class BreezeExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val chatUserBubble: Color,
    val chatUserText: Color,
    val chatAiBubble: Color,
    val chatAiText: Color,
    val chatInputBackground: Color,
    val chatInputBorder: Color,
    val codeBlockBackground: Color,
    val codeBlockBorder: Color,
)

internal val LightBreezeExtendedColors = BreezeExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    info = InfoLight,
    onInfo = OnInfoLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    textDisabled = TextDisabledLight,
    chatUserBubble = BreezePrimary,
    chatUserText = Color.White,
    chatAiBubble = Color.White,
    chatAiText = TextPrimaryLight,
    chatInputBackground = Color.White,
    chatInputBorder = Color(0xFFE5E7EB),
    codeBlockBackground = Color(0xFFF5F7FA),
    codeBlockBorder = Color(0xFFE5E7EB),
)

internal val DarkBreezeExtendedColors = BreezeExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    info = InfoDark,
    onInfo = OnInfoDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    textDisabled = TextDisabledDark,
    chatUserBubble = BreezePrimaryDark,
    chatUserText = Color(0xFF002D5C),
    chatAiBubble = Color(0xFF1A1F25),
    chatAiText = TextPrimaryDark,
    chatInputBackground = Color(0xFF1A1F25),
    chatInputBorder = Color(0xFF2A313A),
    codeBlockBackground = Color(0xFF0F1419),
    codeBlockBorder = Color(0xFF2A313A),
)

internal val LocalBreezeExtendedColors = staticCompositionLocalOf { LightBreezeExtendedColors }
