package com.hrm.breeze.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

enum class WidthClass { Compact, Medium, Expanded }
enum class HeightClass { Compact, Medium, Expanded }
enum class PaneMode { Single, ListDetail, Triple }

@Immutable
data class WindowInfo(
    val widthClass: WidthClass,
    val heightClass: HeightClass,
    val isTouchPreferred: Boolean,
    val contentMaxWidth: Dp,
    val paneMode: PaneMode,
) {
    companion object {
        /** 提供一份兜底值，避免未装配时崩溃。 */
        val Default = WindowInfo(
            widthClass = WidthClass.Compact,
            heightClass = HeightClass.Medium,
            isTouchPreferred = true,
            contentMaxWidth = 720.dp,
            paneMode = PaneMode.Single,
        )
    }
}

val LocalWindowInfo = staticCompositionLocalOf { WindowInfo.Default }

fun WindowInfo.Companion.from(
    windowSizeClass: WindowSizeClass,
    isTouchPreferred: Boolean,
    contentMaxWidth: Dp,
): WindowInfo {
    val widthClass = windowSizeClass.toBreezeWidthClass()
    val heightClass = windowSizeClass.toBreezeHeightClass()
    val paneMode = when (widthClass) {
        WidthClass.Compact -> PaneMode.Single
        WidthClass.Medium -> PaneMode.Single
        WidthClass.Expanded -> PaneMode.ListDetail
    }
    return WindowInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        isTouchPreferred = isTouchPreferred,
        contentMaxWidth = contentMaxWidth,
        paneMode = paneMode,
    )
}

private fun WindowSizeClass.toBreezeWidthClass(): WidthClass = when {
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WidthClass.Expanded
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WidthClass.Medium
    else -> WidthClass.Compact
}

private fun WindowSizeClass.toBreezeHeightClass(): HeightClass = when {
    isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> HeightClass.Expanded
    isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> HeightClass.Medium
    else -> HeightClass.Compact
}
