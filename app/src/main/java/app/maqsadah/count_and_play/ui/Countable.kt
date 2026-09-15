package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.maqsadah.count_and_play.core.ShapeKind

/**
 * One countable. Optionally on a rounded seat (the ADD bowl's part colors),
 * optionally wearing its count chip, and tappable only when [onTap] is given.
 *
 * [touchTarget] is the smallest node the finger gets. A full-width tray can
 * afford [HitTarget]; the half-width ADD plates pack two to a row on the
 * narrowest phone only at [PlateHitTarget], and the layout solver plans rows
 * against exactly this number, so it must be passed through here too.
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
    modifier: Modifier = Modifier,
    touchTarget: Dp = HitTarget,
    chip: String? = null,
    seat: Color? = null,
    gone: Boolean = false,
    label: String? = null,
    onTap: (() -> Unit)? = null,
) {
    val press = rememberObjectPress()
    val vanish = rememberVanish(gone)
    Box(
        modifier = modifier
            .sizeIn(minWidth = touchTarget, minHeight = touchTarget)
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
            .tapToCount(press.source, onTap)
            .objectSemantics(label),
        contentAlignment = Alignment.Center,
    ) {
        CountableArt(shape, sizeDp, seat, chip, vanish)
    }
}

/** The finger's own state: the source a tap listens on and the squash it shows. */
private class ObjectPress(val source: MutableInteractionSource, val scale: Float)

@Composable
private fun rememberObjectPress(): ObjectPress {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    // The press itself is the feedback: squash to 0.85 under the finger, then
    // a spring back. No sound is owned by the view, no ripple dims the candy.
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "objectPress",
    )
    return ObjectPress(interactionSource, scale)
}

/**
 * A tap always lands, even on an emptied slot: the finger is answered with
 * the tactile tick and the core records the reach. Nothing in play is ever a
 * dead surface.
 */
@Composable
private fun Modifier.tapToCount(source: MutableInteractionSource, onTap: (() -> Unit)?): Modifier {
    val tick = rememberTick()
    return if (onTap == null) {
        this
    } else {
        this.clickable(interactionSource = source, indication = null) {
            tick()
            onTap()
        }
    }
}

/** A named countable speaks its name; an emptied slot keeps none and no focus. */
private fun Modifier.objectSemantics(label: String?): Modifier =
    if (label != null) {
        semantics { contentDescription = label }
    } else {
        // An emptied slot is not a thing to count: it keeps no spoken name
        // and no focus, so nobody tries to count it.
        clearAndSetSemantics { }
    }

/**
 * How present an object is while it is being taken away: 0 at rest, 1 once it
 * has sunk into the dashed ghost that keeps its slot.
 */
@Composable
private fun rememberVanish(gone: Boolean): Animatable<Float, AnimationVector1D> {
    val reducedMotion = rememberReducedMotion()
    val vanish = remember { Animatable(if (gone) 1f else 0f) }
    LaunchedEffect(gone, reducedMotion) {
        when {
            !gone -> vanish.snapTo(0f)
            reducedMotion -> vanish.snapTo(1f)
            vanish.value < 1f -> vanish.animateTo(1f, tween(durationMillis = 900))
        }
    }
    return vanish
}

/** The ghost slot first, the countable over it, cross-faded as it sinks away. */
@Composable
private fun CountableArt(
    shape: ShapeKind,
    sizeDp: Dp,
    seat: Color?,
    chip: String?,
    vanish: Animatable<Float, AnimationVector1D>,
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

/** Seat, body and chip of one countable, at full presence. */
@Composable
private fun CountableContent(shape: ShapeKind, sizeDp: Dp, seat: Color?, chip: String?) {
    Box(contentAlignment = Alignment.Center) {
        if (seat != null) {
            Box(Modifier.size(sizeDp * SeatScale).background(seat, CircleShape))
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
