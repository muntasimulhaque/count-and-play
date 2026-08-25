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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.core.ShapeKind

private val RimWidth = 8.dp

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
                    Modifier.clickable(interactionSource = interactionSource, indication = null) { onTap() }
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
            CountChip(chip, dia, Modifier.align(Alignment.TopEnd).offset(x = dia * 0.2f, y = -dia * 0.2f))
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

/** The dashed outline left where a taken-away object used to sit. */
@Composable
fun GhostSlot(sizeDp: Dp) {
    Canvas(Modifier.size(sizeDp)) {
        drawEmptySlot(size.minDimension)
    }
}

/**
 * The white tray the objects live on: liner inside, thick candy rim around,
 * and rows packed exactly as the solver arranged them, so the balanced
 * arrangement computed in [TrayMath] is the arrangement the child sees.
 *
 * The caller passes a solved [layout]: COUNT and TAKE solve their single
 * tray against the room the screen offers, and ADD solves its plates and
 * bowl together in [solveAddTraySizes] so the whole round always fits.
 */
@OptIn(ExperimentalLayoutApi::class) // the arrangement needs maxItemsInEachRow
@Composable
internal fun Tray(
    rim: Color,
    count: Int,
    layout: TraySolution,
    modifier: Modifier = Modifier,
    content: @Composable (objectSize: Dp) -> Unit,
) {
    val size = layout.size
    Box(
        modifier
            // An emptied plate must still look like a place, not vanish.
            .sizeIn(minHeight = size + TrayPad * 2)
            .background(Liner, RoundedCornerShape(Corner))
            .border(BorderStroke(RimWidth, rim), RoundedCornerShape(Corner))
            .padding(TrayPad),
        contentAlignment = Alignment.Center,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TrayGap),
            verticalArrangement = Arrangement.spacedBy(TrayGap),
            maxItemsInEachRow = layout.perRow,
        ) {
            content(size)
        }
    }
}
