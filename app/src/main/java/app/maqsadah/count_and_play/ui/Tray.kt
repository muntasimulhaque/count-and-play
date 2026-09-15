package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pressed-well surface: warm paper fill under an inner top shadow, hairline
 * rim, one tight contact shadow. Wells HOLD, unlike keys that float: they sit
 * flush in the ground like the trays they are, and light from above lands on
 * their far rim, quietly darkening just the inside top edge.
 */
@Composable
internal fun WellSurface(
    tint: Color?,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val fill = tint?.copy(alpha = 0.07f)?.compositeOver(WellFill) ?: WellFill
    Box(
        modifier
            .shadow(elevation = ContactShadow, shape = RoundedCornerShape(Corner), clip = false)
            .background(fill, RoundedCornerShape(Corner))
            .drawBehind {
                val bandPx = InnerRimDepth.toPx().coerceAtMost(size.height)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Ink.copy(alpha = 0.06f), Color.Transparent),
                        startY = 0f,
                        endY = bandPx,
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, bandPx),
                    cornerRadius = CornerRadius(Corner.toPx()),
                )
            }
            .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(Corner)),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** How far down inside a well the caught light fades out. */
private val InnerRimDepth = 10.dp

/**
 * The white tray the objects live on: a WELL pressed into the paper (see
 * [WellSurface] for the material), its rows packed exactly as the solver
 * arranged them, so the balanced arrangement computed in [TrayMath] is the
 * arrangement the child sees. [tint] quietly washes a place that means
 * something different (the TAKE taken-away box) without adding a second
 * chrome color.
 *
 * The caller passes a solved [layout]: COUNT and TAKE solve their single
 * tray against the room the screen offers, and ADD solves its plates and
 * bowl together in [solveAddTraySizes] so the whole round always fits.
 */
@Composable
internal fun Tray(
    count: Int,
    layout: TraySolution,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable (objectSize: Dp) -> Unit,
) {
    val size = layout.size
    WellSurface(
        tint,
        // An emptied plate must still look like a place, not vanish.
        modifier.sizeIn(minHeight = size + TrayPad * 2),
    ) {
        TrayRows(perRow = layout.perRow, modifier = Modifier.padding(TrayPad)) {
            content(size)
        }
    }
}

/**
 * The tray's rows, placed by hand rather than left to a flow layout. A flow
 * measures items in whole pixels, so an exactly-fitting row can push its last
 * item to the next line: the solver says four-across and a phone renders
 * three-and-one, and the whole tray reflows taller. Placing the rows we have
 * already solved removes that class of drift entirely: exactly [perRow]
 * items per full row on every device, the remainder centered beneath, so a
 * five reads as the classic 3-over-2 instead of a lopsided 3-plus-2.
 */
@Composable
private fun TrayRows(perRow: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val plan = rowPlan(measurables.size, perRow)
            val placeables = measurables.map { it.measure(constraints) }
            val gap = TrayGap.roundToPx()
            val rows = buildList {
                var start = 0
                for (n in plan) {
                    add(placeables.subList(start, start + n))
                    start += n
                }
            }
            val rowWidths = rows.map { row -> row.sumOf { it.width } + gap * (row.size - 1) }
            val rowHeights = rows.map { row -> row.maxOf { it.height } }
            val widest = rowWidths.maxOrNull() ?: 0
            val height = if (rows.isEmpty()) 0 else rowHeights.sum() + gap * (rows.size - 1)
            val width = widest.coerceIn(constraints.minWidth, constraints.maxWidth)
            layout(width, height) {
                var y = 0
                rows.forEachIndexed { index, row ->
                    var x = (width - rowWidths[index]) / 2
                    row.forEach { item ->
                        item.place(x, y + (rowHeights[index] - item.height) / 2)
                        x += item.width + gap
                    }
                    y += rowHeights[index] + gap
                }
            }
        },
    )
}
