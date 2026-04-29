package com.hrm.breeze.ui.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo as ComposeLocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberWindowInfo(
    isTouchPreferred: Boolean = platformPrefersTouchInput(),
): WindowInfo {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val containerDpSize = ComposeLocalWindowInfo.current.containerDpSize
    val contentMaxWidth = remember(adaptiveInfo.windowSizeClass, containerDpSize.width) {
        when {
            adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
            ) -> 840.dp
            adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
            ) -> 720.dp
            else -> containerDpSize.width
        }
    }

    return remember(adaptiveInfo.windowSizeClass, contentMaxWidth, isTouchPreferred) {
        WindowInfo.from(
            windowSizeClass = adaptiveInfo.windowSizeClass,
            isTouchPreferred = isTouchPreferred,
            contentMaxWidth = contentMaxWidth,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ProvideWindowInfo(
    content: @Composable () -> Unit,
) {
    val windowInfo = rememberWindowInfo()
    CompositionLocalProvider(
        LocalWindowInfo provides windowInfo,
        content = content,
    )
}

expect fun platformPrefersTouchInput(): Boolean
