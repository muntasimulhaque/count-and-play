package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.core.ShapeKind

// Room for two digits ("10" / "১০") without going oval: the chip grows with
// the object it tags, from a floor that stays legible on small trays.
private fun chipDiameter(objectSize: Dp): Dp = maxOf(30.dp, objectSize * 0.36f)

/**
 * One countable. Optionally on a rounded seat (the ADD bowl's part colours),
 * optionally wearing its count chip, and tappable only when [onTap] is given.
 *
 * [gone] is the TAKE removal: the same node first shows the object sinking
 * away, then the dashed ghost that keeps its slot. Keeping one composable per
 * slot across that transition preserves its identity, so screen-reader focus
 * never resets mid-round.
 */
@Composable
fun ObjectView(
    shape: ShapeKind,
    sizeDp: Dp,
    chip: String? = null,
    seat: Color? = null,
    gone: Boolean = false,
    label: String? = null,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val tick = rememberTick()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // The press itself is the feedback: squash to 0.85 under the finger, then
    // a spring back. No sound is owned by the view, no ripple dims the candy.
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "objectPress",
    )
    val vanish = remember { Animatable(if (gone) 1f else 0f) }
    LaunchedEffect(gone, reducedMotion) {
        when {
            !gone -> vanish.snapTo(0f)
            reducedMotion -> vanish.snapTo(1f)
            vanish.value < 1f -> vanish.animateTo(1f, tween(durationMillis = 900))
        }
    }
    Box(
        modifier = modifier
            .sizeIn(minWidth = HitTarget, minHeight = HitTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (onTap != null && !gone) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) {
                        tick()
                        onTap()
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { alpha = vanish.value }) {
            Canvas(Modifier.size(sizeDp)) { drawEmptySlot(size.minDimension) }
        }
        Box(
            Modifier.graphicsLayer {
                alpha = 1f - vanish.value
                val s = 1f - 0.5f * vanish.value
                scaleX = s
                scaleY = s
            },
        ) {
            CountableContent(shape, sizeDp, seat, chip)
        }
    }
}

/** Seat, body and chip of one countable, at full presence. */
@Composable
private fun CountableContent(shape: ShapeKind, sizeDp: Dp, seat: Color?, chip: String?) {
    Box(contentAlignment = Alignment.Center) {
        if (seat != null) {
            Box(Modifier.size(sizeDp * 1.3f).background(seat, CircleShape))
        }
        Canvas(Modifier.size(sizeDp)) {
            drawCountable(shape, size.minDimension, detailFor(sizeDp.value))
        }
        if (chip != null) {
            val dia = chipDiameter(sizeDp)
            // The chip lands with its own little pop: his tap made a number
            // exist, and the number celebrates that too. The pop wraps the
            // chip from outside, so the chip keeps its top-right seat.
            key(chip) {
                PopIn(Modifier.align(Alignment.TopEnd).offset(x = dia * 0.2f, y = -dia * 0.2f)) {
                    CountChip(chip, dia)
                }
            }
        }
    }
}

/**
 * The numbered chip in the child's own tap order. A fixed square, so it stays
 * a true circle even for two-digit chips like 10: aspectRatio under loose
 * constraints would balloon it to fill the tray.
 */
@Composable
private fun CountChip(text: String, diameter: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(diameter)
            .background(ChipBlue, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = (diameter.value * 0.58f).sp,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

/**
 * The dashed outline left where a taken-away object used to sit. [node] is
 * the slot's layout footprint: it must match the node an ObjectView occupies
 * in the same tray, so rows keep one rhythm whether a cell holds an object
 * or the ghost of one. The dash itself stays at the body size, deliberately
 * quiet so it is not read as an object.
 */
@Composable
fun GhostSlot(sizeDp: Dp, node: Dp = sizeDp) {
    Box(Modifier.size(node), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            drawEmptySlot(size.minDimension)
        }
    }
}

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
 * chrome colour.
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
 * items per full row on every device, the remainder centred beneath, so a
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
