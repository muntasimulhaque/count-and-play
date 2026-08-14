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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.core.ShapeKind

private val RimWidth = 8.dp
private val TrayPad = 14.dp
private val TrayGap = 10.dp
private val ChipDiameter = 26.dp

// A three-year-old's finger lands with a wide margin; anything tappable gets
// at least this much target, whatever the visible object inside it measures.
private val HitTarget = 72.dp

/**
 * One countable. Optionally on a rounded seat (the ADD bowl's part colours),
 * optionally wearing its count chip, and tappable only when [onTap] is given.
 */
@Composable
fun ObjectView(
    shape: ShapeKind,
    sizeDp: Dp,
    chip: String? = null,
    seat: Color? = null,
    onTap: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // The press itself is the feedback: squash to 0.85 under the finger, then
    // a spring back. No sound is owned by the view, no ripple dims the candy.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "objectPress",
    )
    Box(
        modifier = Modifier
            .sizeIn(minWidth = HitTarget, minHeight = HitTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (onTap != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) { onTap() }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (seat != null) {
                Box(Modifier.size(sizeDp * 1.3f).background(seat, CircleShape))
            }
            Canvas(Modifier.size(sizeDp)) {
                drawCountable(shape, size.minDimension, detailFor(sizeDp.value))
            }
            if (chip != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .sizeIn(minWidth = ChipDiameter, minHeight = ChipDiameter)
                        .background(ChipBlue, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip, color = Color.White, fontSize = SizeChip, fontWeight = ToyBlack, fontFamily = ToyFont)
                }
            }
        }
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
 * A just-taken object: it lingers a beat wearing its take-away number, then
 * shrinks into the dashed ghost that keeps its slot for the rest of the
 * round. This is a visual transition (an animation), not pacing: the count
 * words and their timing stay with the host's beats.
 */
@Composable
fun TakenSlot(shape: ShapeKind, sizeDp: Dp, chip: String?) {
    val vanish = remember { Animatable(0f) }
    LaunchedEffect(Unit) { vanish.animateTo(1f, tween(durationMillis = 900)) }
    Box(
        Modifier.sizeIn(minWidth = HitTarget, minHeight = HitTarget),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { alpha = vanish.value }) { GhostSlot(sizeDp) }
        Box(
            Modifier.graphicsLayer {
                val p = vanish.value
                alpha = 1f - p
                val s = 1f - 0.5f * p
                scaleX = s
                scaleY = s
            },
        ) {
            ObjectView(shape = shape, sizeDp = sizeDp, chip = chip)
        }
    }
}

/**
 * The white tray the objects live on: liner inside, thick candy rim around,
 * and rows of five so the five-frame stays visible in every arrangement.
 * [count] is the number of slots shown and picks the object size.
 */
@OptIn(ExperimentalLayoutApi::class) // the five-frame needs maxItemsInEachRow
@Composable
fun Tray(
    rim: Color,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (objectSize: Dp) -> Unit,
) {
    val objectSize = if (count <= 5) 96.dp else 64.dp
    Box(
        modifier
            // An emptied plate must still look like a place, not vanish.
            .sizeIn(minHeight = objectSize + TrayPad * 2)
            .background(Liner, RoundedCornerShape(Corner))
            .border(BorderStroke(RimWidth, rim), RoundedCornerShape(Corner))
            .padding(TrayPad),
        contentAlignment = Alignment.Center,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TrayGap),
            verticalArrangement = Arrangement.spacedBy(TrayGap),
            maxItemsInEachRow = 5,
        ) {
            content(objectSize)
        }
    }
}
