package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.Token

/**
 * One plate of objects. A sleeping plate keeps its place and its rim color
 * but its pieces are drawn washed-out and monochrome, so the child can see
 * which column is his before its turn arrives. It still answers the finger:
 * the core turns a tap there into a soft tick and records the reach, never a
 * count, so no touch in play is ever dead. A finished plate wears its total
 * as a popped-on badge that survives the pour.
 */
@Composable
internal fun PlateTray(
    rim: Color,
    tokens: List<Token>,
    objectSize: Dp,
    perRow: Int,
    badge: String?,
    badgeCentered: Boolean,
    awake: Boolean,
    copy: Copy,
    modifier: Modifier,
    onTap: (Int) -> Unit,
) {
    Box(modifier) {
        Tray(
            tokens.size,
            TraySolution(objectSize, perRow),
            Modifier.fillMaxWidth(),
            // The plate's own hue, so the left and right columns are told
            // apart at a glance and match the seats they fill in the bowl.
            tint = rim,
        ) { size ->
            val washout = Modifier.plateWashout(awake)
            tokens.forEach { token -> PlatePiece(token, size, washout, copy, onTap) }
        }
        PlateBadge(badge, rim, objectSize, badgeCentered)
    }
}

/** One piece of a plate, drawn washed-out while its column is still asleep. */
@Composable
private fun PlatePiece(token: Token, size: Dp, washout: Modifier, copy: Copy, onTap: (Int) -> Unit) {
    key(token.id) {
        ObjectView(
            shape = token.shape,
            sizeDp = size,
            modifier = washout,
            touchTarget = PlateHitTarget,
            chip = if (token.counted) copy.digits(token.countOrder) else null,
            label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
            onTap = { onTap(token.id) },
        )
    }
}

/** The sleeping column's wash: pale and monochrome, gone once it is awake. */
private fun Modifier.plateWashout(awake: Boolean): Modifier =
    if (awake) {
        this
    } else {
        graphicsLayer {
            alpha = 0.4f
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(0f) })
        }
    }

/** The plate's finished total: a tag hanging off the corner no piece reaches. */
@Composable
private fun BoxScope.PlateBadge(badge: String?, ring: Color, objectSize: Dp, centered: Boolean) {
    if (badge == null) return
    val dia = badgeDiameter(objectSize)
    // Standing plates hang the total off the well's bottom-right corner:
    // the one spot the pieces never reach (count chips sit at each object's
    // top-right), so the badge is a tag on the plate rather than a lid over
    // the last piece. Folded places hold the total centered, where there is
    // nothing else left to cover.
    val place = if (centered) {
        Modifier.align(Alignment.Center)
    } else {
        Modifier.align(Alignment.BottomEnd).offset(x = BadgeOverhang, y = BadgeOverhang)
    }
    TotalBadge(badge, ring, dia, place)
}

/** The plate's finished total: a candy disc with the numeral, popping onto the plate. */
@Composable
private fun TotalBadge(text: String, ring: Color, diameter: Dp, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.4f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && scale.value < 1f) scale.animateTo(1f, BadgeSpring)
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .size(diameter)
            .shadow(elevation = LiftHeld, shape = CircleShape, clip = false)
            .background(Liner, CircleShape)
            .border(BorderStroke(3.dp, ring), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = flashTint(ring),
            fontSize = (diameter.value * 0.52f).sp,
            lineHeight = (diameter.value * 0.52f).sp,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

/** The darker fact-card variant of a tray rim, for legible numerals on white. */
private fun flashTint(rim: Color): Color = when (rim) {
    Blue -> FlashBlue
    Orange -> FlashOrange
    Pink -> FlashPink
    else -> Ink
}

private val BadgeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)
