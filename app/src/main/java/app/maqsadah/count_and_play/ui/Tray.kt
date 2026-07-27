package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.Token
import kotlin.math.ceil
import kotlin.math.min

/** Five to a row: a child sees seven as "five and two more". */
const val FRAME_COLS = 5

/**
 * A tray of countable objects.
 *
 * The touch target is the **whole tray**, partitioned by nearest object. That
 * single decision retires an entire class of defect: the old build put the
 * click listener on the emoji glyph *inside* the layer that animated it, so for
 * the first half-second of every round the real target was scaled to 0.3 while
 * the child was already reaching for it — well under half Android's adult
 * minimum, on an object about 5 mm across.
 *
 * Taps commit on press, not release, and a second finger is ignored.
 */
@Composable
fun Tray(
    tokens: List<Token>,
    capacity: Int,
    revealed: Set<Int>,
    highlighted: Set<Int>,
    copy: Copy,
    modifier: Modifier = Modifier,
    label: String = "",
    showEmptySlots: Boolean = false,
    /** The collapsed cardinal, shown on this tray's rim once counting ends. */
    cardinal: String? = null,
    onTapToken: (Int) -> Unit = {},
    onTapEmpty: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val slots = maxOf(capacity, tokens.size, 1)
        val cols = min(slots, FRAME_COLS)
        val rows = ceil(slots / cols.toFloat()).toInt()
        val pad = 10.dp

        // One cell size, derived arithmetically from the space and the count.
        // Overflow is therefore impossible — the old build estimated how many
        // items would fit and let a flow layout wrap into a clipped row, so
        // objects silently vanished (8 + 8 showed six balls).
        val cell: Dp = min(
            (maxWidth - pad * 2).value / cols,
            (maxHeight - pad * 2).value / rows,
        ).dp.coerceIn(24.dp, 128.dp)

        val cellPx = with(density) { cell.toPx() }
        val padPx = with(density) { pad.toPx() }

        fun centreOf(slot: Int) = Offset(
            padPx + (slot % cols + 0.5f) * cellPx,
            padPx + (slot / cols + 0.5f) * cellPx,
        )

        val placed = remember(tokens) { tokens.sortedBy { it.slot } }

        // The tray hugs its grid rather than stretching to the whole slot, so a
        // set of three is a small tray of three rather than three objects lost
        // in an acre of wood.
        Box(
            Modifier
                .align(Alignment.Center)
                .size(cell * cols + pad * 2, cell * rows + pad * 2)
                .background(palette.tray, RoundedCornerShape(22.dp))
                .border(3.dp, palette.trayRim, RoundedCornerShape(22.dp))
                .semantics { contentDescription = label }
                .pointerInput(placed, cols) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        // Nearest object wins, so there is no small target to
                        // miss and a fat finger between two objects still picks
                        // the one it was closest to.
                        val nearest = placed.minByOrNull { token ->
                            (centreOf(token.slot) - down.position).getDistanceSquared()
                        }
                        if (nearest != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTapToken(nearest.id)
                        } else {
                            onTapEmpty()
                        }
                        // Any further fingers in this gesture are ignored.
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            if (showEmptySlots) {
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        for (slot in tokens.size until slots) {
                            val c = centreOf(slot)
                            translate(c.x - cellPx / 2f, c.y - cellPx / 2f) {
                                drawEmptySlot(cellPx, palette)
                            }
                        }
                    },
                )
            }

            for (token in placed) {
                TokenView(
                    token = token,
                    centre = centreOf(token.slot),
                    cellPx = cellPx,
                    cellDp = cell.value,
                    revealed = token.id in revealed,
                    breathing = token.id in highlighted,
                    copy = copy,
                    measurer = measurer,
                )
            }

            // The many tags have become one number, and it sits on the rim of
            // the tray that holds them — attached to the set it describes.
            if (cardinal != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = (-18).dp)
                        .background(palette.trayLiner, RoundedCornerShape(50))
                        .border(3.dp, palette.ink, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = cardinal,
                        color = palette.ink,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenView(
    token: Token,
    centre: Offset,
    cellPx: Float,
    cellDp: Float,
    revealed: Boolean,
    breathing: Boolean,
    copy: Copy,
    measurer: androidx.compose.ui.text.TextMeasurer,
) {
    val palette = LocalPalette.current
    val colors = colorsFor(token.shape)
    val detail = detailFor(cellDp)

    // Objects arrive from off-stage at full size. Scaling up from nothing reads
    // as materialising out of thin air, which quietly undermines the idea that
    // nothing is created or destroyed.
    val entry by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 320f),
        label = "entry",
    )
    val pop by animateFloatAsState(
        targetValue = if (token.isCounted) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "pop",
    )
    val breathe = if (breathing) {
        val transition = rememberInfiniteTransition(label = "breathe")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "breathe",
        ).value
    } else {
        1f
    }

    if (entry <= 0.01f) return

    Box(
        Modifier.fillMaxSize().drawBehind {
            val scale = breathe * (0.96f + 0.04f * entry) * (1f + 0.14f * pop * (1f - pop) * 4f)
            val drawn = cellPx * 0.80f * scale
            val left = centre.x - drawn / 2f
            val top = centre.y - drawn / 2f + (1f - entry) * cellPx * 0.9f

            translate(left, top) {
                rotate(tiltFor(token.slot), pivot = Offset(drawn / 2f, drawn / 2f)) {
                    drawCountable(token.shape, drawn, colors, palette, detail)
                }
            }

            token.ordinal?.let { ordinal ->
                // Three cues at once — a ring, a numbered chip and a pop — so
                // "counted" never depends on colour alone.
                drawCircle(
                    color = palette.countedRing,
                    radius = drawn * 0.54f,
                    center = centre,
                    style = Stroke(width = drawn * 0.05f),
                )
                val chipR = drawn * 0.20f
                val chipAt = Offset(centre.x + drawn * 0.34f, centre.y - drawn * 0.34f)
                drawCircle(palette.countedChip, chipR, chipAt)
                val text = measurer.measure(
                    copy.digits(ordinal),
                    style = TextStyle(
                        color = palette.onCountedChip,
                        fontSize = (chipR * 1.5f / density).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                drawText(
                    text,
                    topLeft = Offset(
                        chipAt.x - text.size.width / 2f,
                        chipAt.y - text.size.height / 2f,
                    ),
                )
            }
        },
    )
}
