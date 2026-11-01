package com.measurepulse.ar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Lets a horizontally scrolling row inside padded content run to the screen edges, so chips that
 * don't fit are cut by the screen (reads as "scroll for more") instead of by the content padding.
 * Pair it with contentPadding of the same [bleed] on the row.
 */
fun Modifier.edgeBleed(bleed: Dp): Modifier = layout { measurable, constraints ->
    val px = bleed.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + 2 * px, maxWidth = constraints.maxWidth + 2 * px)
    )
    layout(placeable.width - 2 * px, placeable.height) { placeable.place(-px, 0) }
}
