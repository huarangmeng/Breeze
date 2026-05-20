package com.hrm.breeze.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Stable
class AutoScrollToBottomState internal constructor(
    private val scrollState: ScrollState,
    private val bottomThresholdPx: Int,
) {
    private var isProgrammaticScrollInFlight: Boolean = false

    var shouldAutoScroll by mutableStateOf(true)
        private set

    fun enableAutoScroll() {
        shouldAutoScroll = true
    }

    fun disableAutoScroll() {
        shouldAutoScroll = false
    }

    internal fun updateFromScrollPosition() {
        if (isProgrammaticScrollInFlight) {
            return
        }
        shouldAutoScroll = scrollState.maxValue - scrollState.value <= bottomThresholdPx
    }

    suspend fun scrollToBottom(force: Boolean = false) {
        if (!force && !shouldAutoScroll) {
            return
        }
        shouldAutoScroll = true
        isProgrammaticScrollInFlight = true
        try {
            // Wait for layout to settle so the latest maxValue is visible before forcing bottom.
            withFrameNanos { }
            scrollState.scrollTo(scrollState.maxValue)
        } finally {
            isProgrammaticScrollInFlight = false
        }
    }
}

@Composable
fun rememberAutoScrollToBottomState(
    scrollState: ScrollState,
    bottomThresholdPx: Int = 48,
): AutoScrollToBottomState {
    val state = remember(scrollState, bottomThresholdPx) {
        AutoScrollToBottomState(
            scrollState = scrollState,
            bottomThresholdPx = bottomThresholdPx,
        )
    }
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collectLatest {
                latestState.updateFromScrollPosition()
            }
    }

    return state
}
