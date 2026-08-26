package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The shared geometry of every key: one radius, one hairline. */
private val KeyShape = RoundedCornerShape(Corner)

/** Sink and shadow travel on one spring, so the depth stays coherent. */
private val KeySpring = spring<Dp>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * A pressable toy key, hung in the calm gallery: a white cap riding on a
 * neutral sand side edge. At rest the cap sits lifted and floats on its
 * shadow; under the finger it sinks flush, the shadow vanishes and the edge
 * disappears, so a press is felt as much as seen. Solid colours only: the
 * depth is geometry, not a gradient. Caps are white; [fill] overrides. Keys
 * FLOAT, wells (see Objects) hold.
 *
 * The lift lives inside the key's own top padding, so callers lay keys out
 * exactly like plain boxes and neighbours never jump when one sinks.
 */
@Composable
fun Keycap(
    edge: Color,
    modifier: Modifier = Modifier,
    edgeHeight: Dp = 9.dp,
    /** True: fill whatever width the caller grants. False: hug the content. */
    stretch: Boolean = true,
    description: String? = null,
    /** Spoken state when the key is present but asleep ("not yet"). */
    stateDescription: String? = null,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    fill: Color = Liner,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val active = pressed && !reducedMotion && onClick != null
    val sink by animateDpAsState(if (active) edgeHeight else 0.dp, KeySpring, label = "keycapSink")
    val lift by animateDpAsState(if (active) LiftHeld else LiftResting, KeySpring, label = "keycapLift")
    val tick = rememberTick()
    val sizeModifier = if (stretch) Modifier.fillMaxSize() else Modifier
    Box(modifier.padding(top = edgeHeight)) {
        // The side of the key: the neutral sand shows exactly where the cap sits lifted.
        Box(sizeModifier.background(edge, KeyShape))
        Box(
            sizeModifier
                .offset(y = sink - edgeHeight)
                .shadow(elevation = lift, shape = KeyShape)
                .background(fill, KeyShape)
                .border(BorderStroke(1.dp, Hairline), KeyShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(interactionSource, indication = null) {
                            tick()
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                )
                .semantics {
                    role = Role.Button
                    if (description != null) contentDescription = description
                    if (stateDescription != null) this.stateDescription = stateDescription
                },
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * The quiet controls' press feedback (gear, home, close, the grown-up rows):
 * a small sink under the finger and one light tick, no ripple anywhere.
 * Chain it ahead of the control's own background so the whole control scales.
 */
@Composable
fun Modifier.pressable(scaleDown: Float = 0.94f, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) scaleDown else 1f,
        animationSpec = QuietSpring,
        label = "quietPress",
    )
    val tick = rememberTick()
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null) {
            tick()
            onClick()
        }
}
