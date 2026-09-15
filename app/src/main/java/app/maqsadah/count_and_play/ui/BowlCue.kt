package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy

/**
 * The wake bloom: washed and desaturated while the plates are still being
 * counted, blooming to full presence when the bowl becomes the way forward.
 * One morph, never a swap; a bowl that comes alive reads as alive.
 */
@Composable
internal fun Modifier.bowlWash(awake: Boolean): Modifier {
    val alpha by animateFloatAsState(if (awake) 1f else 0.4f, tween(300), label = "bowlAlpha")
    val saturation by animateFloatAsState(if (awake) 1f else 0f, tween(300), label = "bowlSat")
    return graphicsLayer {
        this.alpha = alpha
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(saturation) })
    }
}

/**
 * The sleeping bowl as a button: role, spoken name (the ask itself), spoken
 * state (ready or not yet), and a tap that always answers: the pour when it
 * is time, the soft tick through the host when it is not. The tick here is
 * the finger's answer; the sound beat comes from the core.
 */
@Composable
internal fun Modifier.bowlTap(ready: Boolean, copy: Copy, onPour: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val tick = rememberTick()
    return this
        .clickable(interactionSource = interaction, indication = null) {
            tick()
            onPour()
        }
        .semantics {
            role = Role.Button
            contentDescription = copy.promptAdd()
            stateDescription = if (ready) copy.pourReadyState() else copy.pourNotYetState()
        }
}

/**
 * The words on the bowl itself: once both plates are counted, the destination
 * says what it is for, so the cue sits where the finger goes and never
 * depends on the voice. It pops in with the wake bloom and breathes gently
 * until the pour takes it away; reduced motion holds it still.
 */
@Composable
internal fun PourCue(text: String, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val breath = remember { Animatable(if (reducedMotion) 1f else 0.92f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            breath.animateTo(
                1.06f,
                infiniteRepeatable(tween(durationMillis = 850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            )
        } else {
            breath.snapTo(1f)
        }
    }
    Box(
        modifier
            .graphicsLayer { scaleX = breath.value; scaleY = breath.value }
            .shadow(elevation = LiftHeld, shape = RoundedCornerShape(CornerSmall), clip = false)
            .background(Liner, RoundedCornerShape(CornerSmall))
            .border(BorderStroke(2.dp, Blue), RoundedCornerShape(CornerSmall))
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // The darkened fact-card variant of Blue: legible on white at a glance.
        Text(
            text,
            color = FlashBlue,
            fontSize = 17.sp,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

/*
 * The pour button is gone: the bowl below the plates IS the button now, so
 * the words ask from the headline and the destination receives the tap.
 */
