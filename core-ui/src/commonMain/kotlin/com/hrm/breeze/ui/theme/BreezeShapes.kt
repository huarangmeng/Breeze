package com.hrm.breeze.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class BreezeBubbleShapes(
    val incoming: RoundedCornerShape,
    val outgoing: RoundedCornerShape,
)

@Immutable
data class BreezeShapes(
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val pill: RoundedCornerShape,
    val input: RoundedCornerShape,
    val codeBlock: RoundedCornerShape,
    val bubbles: BreezeBubbleShapes,
) {
    fun asMaterialShapes(): Shapes = Shapes(
        small = small,
        medium = medium,
        large = large,
    )
}

internal val DefaultBreezeShapes = BreezeShapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    pill = RoundedCornerShape(999.dp),
    input = RoundedCornerShape(16.dp),
    codeBlock = RoundedCornerShape(18.dp),
    bubbles = BreezeBubbleShapes(
        incoming = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 6.dp,
            bottomEnd = 18.dp,
        ),
        outgoing = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 6.dp,
        ),
    ),
)

internal val LocalBreezeShapes = staticCompositionLocalOf { DefaultBreezeShapes }
