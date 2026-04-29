package com.hrm.breeze.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/**
 * 根据宽度 dp 计算 WindowInfo。断点与 AGENTS.md §4 对齐。
 */
fun WindowInfo.Companion.from(widthDp: Int, heightDp: Int, isTouchPreferred: Boolean): WindowInfo {
    val widthClass = when {
        widthDp < 600 -> WidthClass.Compact
        widthDp < 840 -> WidthClass.Medium
        else -> WidthClass.Expanded
    }
    val heightClass = when {
        heightDp < 480 -> HeightClass.Compact
        heightDp < 900 -> HeightClass.Medium
        else -> HeightClass.Expanded
    }
    val paneMode = when (widthClass) {
        WidthClass.Compact -> PaneMode.Single
        WidthClass.Medium -> PaneMode.Single
        WidthClass.Expanded -> PaneMode.ListDetail
    }
    val contentMaxWidth = when (widthClass) {
        WidthClass.Compact -> widthDp.dp
        WidthClass.Medium -> 720.dp
        WidthClass.Expanded -> 840.dp
    }
    return WindowInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        isTouchPreferred = isTouchPreferred,
        contentMaxWidth = contentMaxWidth,
        paneMode = paneMode,
    )
}
