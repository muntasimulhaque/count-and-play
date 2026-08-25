package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pressable toy key: a cap riding on a solid darker side edge. At rest the
 * cap sits lifted and the edge shows, so the key reads as thick; under the
 * finger the cap sinks flush and the edge disappears, so a press is felt as
 * much as seen. Solid colours only: the depth is geometry, not shadow.
 *
 * The lift lives inside the key's own top padding, so callers lay keys out
 * exactly like plain boxes and neighbours never jump when one sinks.
 */
@Composable
fun Keycap(
    rim: Color,
    edge: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    edgeHeight: Dp = 10.dp,
    /** True: fill whatever width the caller grants. False: hug the content. */
    stretch: Boolean = true,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    // Sinking is the only press feedback: no ripple, no dim, just the key
    // going down under the finger and springing back up.
    val sink by animateDpAsState(
        targetValue = if (pressed && !reducedMotion && onClick != null) edgeHeight else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "keycapSink",
    )
    val sizeModifier = if (stretch) Modifier.fillMaxSize() else Modifier
    Box(modifier.padding(top = edgeHeight)) {
        Box(sizeModifier.background(edge, RoundedCornerShape(Corner)))
        Box(
            sizeModifier
                .offset(y = sink - edgeHeight)
                .background(fill, RoundedCornerShape(Corner))
                .border(BorderStroke(OutlineWidth, rim), RoundedCornerShape(Corner))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(interactionSource, indication = null, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (description != null) {
                        Modifier.semantics {
                            role = Role.Button
                            contentDescription = description
                        }
                    } else {
                        Modifier.semantics { role = Role.Button }
                    },
                ),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}
