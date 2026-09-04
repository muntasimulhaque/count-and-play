package app.maqsadah.count_and_play.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True when the device has animations switched off system-wide (animator
 * duration scale 0). Celebrations then land instantly: confetti is skipped
 * and springs snap to their end values, so a vestibular-sensitive grown-up
 * holding the phone is never handed a screen full of moving paper.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** The gentle spring behind every quiet control's pressed response. */
internal val QuietSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** The beat between siblings in a staggered cascade reveal. */
internal const val StaggerStep = 70

/**
 * Rises in with a fade: for a surface that arrives as a whole, such as the
 * bowl sliding in beneath the plates after the pour. Skipped under reduced
 * motion.
 */
@Composable
fun RiseIn(distance: Dp = 36.dp, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val rise = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && rise.value < 1f) {
            rise.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = rise.value
            translationY = (1f - rise.value) * distance.toPx()
        },
    ) { content() }
}

/**
 * One sibling of a staggered cascade: pops in a beat after its predecessors,
 * so a row of glyphs reads left to right the way the voice says it.
 */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && appear.value < 1f) {
            appear.animateTo(
                1f,
                tween(durationMillis = 220, delayMillis = index * StaggerStep, easing = FastOutSlowInEasing),
            )
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = appear.value
            val s = 0.7f + 0.3f * appear.value
            scaleX = s
            scaleY = s
        },
    ) { content() }
}

/** A taken piece or a fresh chip lands with a springy pop; reduced motion snaps. */
@Composable
internal fun PopIn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.3f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && scale.value < 1f) scale.animateTo(1f, PopInSpring)
    }
    Box(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) { content() }
}

private val PopInSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/** How far above its seat a pouring piece starts its drop. */
internal val FallDistance = 56.dp

/** The squash a landed piece recovers from, as a fraction of its height. */
private const val LandSquash = 0.16f

private val LandSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * Falls into its seat under gravity: the drop accelerates (position goes with
 * t squared, the way falling actually works), the piece squashes on impact
 * and springs back to rest. Each sibling in the pour is delayed a beat, so
 * the bowl fills as a cascade rather than a swap. Reduced motion seats the
 * piece instantly: the pour is a courtesy of motion, never a requirement.
 */
@Composable
internal fun FallIn(index: Int, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val fall = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val land = remember { Animatable(1f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            fall.snapTo(1f)
            land.snapTo(1f)
        } else {
            fall.animateTo(1f, tween(durationMillis = 300, delayMillis = index * 45, easing = LinearEasing))
            land.snapTo(0f)
            land.animateTo(1f, LandSpring)
        }
    }
    Box(
        Modifier.graphicsLayer {
            val t = fall.value
            translationY = -(1f - t * t) * FallDistance.toPx()
            val miss = 1f - land.value
            scaleX = 1f + 0.10f * miss
            scaleY = 1f - LandSquash * miss
        },
    ) { content() }
}

/**
 * Fades its content up from nothing, once, over about a third of a second.
 * For layers that arrive with a screen rather than by the child's own tap,
 * such as the first-run door. Skipped entirely under reduced motion.
 */
@Composable
fun FadeIn(content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val fade = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && fade.value < 1f) fade.animateTo(1f, tween(durationMillis = 320))
    }
    Box(Modifier.graphicsLayer { this.alpha = fade.value }) { content() }
}
