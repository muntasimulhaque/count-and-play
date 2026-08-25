package app.maqsadah.count_and_play.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
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
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** The gentle spring behind every quiet control's pressed response. */
internal val QuietSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

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
