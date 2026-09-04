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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState

/**
 * The bowl column, in every phase. Roomy screens hold the full-size bowl
 * from the first frame: ghost seats while he counts the plates, the wake
 * bloom when both are counted, pieces falling into those same seats on the
 * pour. Tight screens keep a slim sleeping strip in phase one and rise the
 * full bowl in with the pour, folding the plates to make its room.
 */
@Composable
internal fun BowlColumn(
    state: AddState,
    copy: Copy,
    sizes: TraySizes,
    onPour: () -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    when {
        !sizes.bowlInPlace && !state.poured ->
            BowlStrip(state, copy, onPour, modifier)
        !sizes.bowlInPlace && state.poured ->
            RiseIn { BowlTray(state, copy, TraySolution(sizes.bowl, sizes.bowlPerRow), onTap) }
        else ->
            BowlInPlace(state, copy, sizes, onPour, onTap, modifier)
    }
}

/**
 * The tight screen's sleeping bowl: a slim well strip with a dashed mouth,
 * washed out until both plates are counted, then awake. A tap answers the
 * finger at any time: the soft tick before it is time, the pour after.
 */
@Composable
private fun BowlStrip(state: AddState, copy: Copy, onPour: () -> Unit, modifier: Modifier) {
    val ready = state.platesReady
    val wash = Modifier.bowlWash(ready)
    WellSurface(
        tint = Green,
        modifier = modifier
            .heightIn(min = BowlAsleepReserve)
            .bowlTap(ready, copy, onPour)
            .then(wash),
    ) {
        GhostSlot(44.dp)
        // When it wakes, the destination says what it is for, right where
        // the finger goes: a cue that survives the sound switch.
        if (ready) PourCue(copy.promptAdd(), Modifier.align(Alignment.Center))
    }
}

/** The bowl the pieces fell into, after the pour on a tight screen. */
@Composable
private fun BowlTray(state: AddState, copy: Copy, layout: TraySolution, onTap: (Int) -> Unit) {
    Tray(state.bowl.size, layout, Modifier.fillMaxWidth()) { size ->
        state.bowl.forEachIndexed { index, token ->
            key(token.id) {
                FallIn(index) {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.counted) copy.digits(token.countOrder) else null,
                        // Each part keeps its plate's colour under it in the bowl.
                        seat = if (token.origin == 1) SeatA else SeatB,
                        label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                        onTap = { onTap(token.id) },
                    )
                }
            }
        }
    }
}

/**
 * The roomy screen's bowl, present from the first frame and constant through
 * the pour: before it, total ghost seats on their part colours, washed out
 * until both plates are counted and the whole bowl is the tap target; after
 * it, the pieces fallen into exactly those seats, each one tappable for the
 * fresh count of the whole. The geometry never changes, so the pour moves
 * pieces, not furniture.
 */
@Composable
private fun BowlInPlace(
    state: AddState,
    copy: Copy,
    sizes: TraySizes,
    onPour: () -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    val wash = if (state.poured) Modifier else Modifier.bowlWash(state.platesReady)
    val tap = if (state.poured) {
        Modifier
    } else {
        Modifier.bowlTap(state.platesReady, copy, onPour)
    }
    Box(modifier.then(tap).then(wash)) {
        Tray(
            state.total,
            TraySolution(sizes.bowl, sizes.bowlPerRow),
            Modifier.fillMaxWidth(),
        ) { size ->
            if (state.poured) {
                state.bowl.forEachIndexed { index, token ->
                    key(token.id) {
                        FallIn(index) {
                            ObjectView(
                                shape = token.shape,
                                sizeDp = size,
                                chip = if (token.counted) copy.digits(token.countOrder) else null,
                                seat = if (token.origin == 1) SeatA else SeatB,
                                label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                                onTap = { onTap(token.id) },
                            )
                        }
                    }
                }
            } else {
                repeat(state.a) { GhostSeat(size, SeatA) }
                repeat(state.b) { GhostSeat(size, SeatB) }
            }
        }
        // The awake bowl wears the ask itself, where the finger goes.
        if (state.platesReady && !state.poured) PourCue(copy.promptAdd(), Modifier.align(Alignment.Center))
    }
}

/** An empty bowl seat: the part's colour under the dashed slot to come. The
 *  node matches the seat a fallen piece occupies, so the pour moves pieces,
 *  never furniture, and both phases of the bowl lay out on one rhythm. */
@Composable
private fun GhostSeat(sizeDp: Dp, seat: Color) {
    Box(Modifier.size(nodeOf(sizeDp, seated = true)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(sizeDp * SeatScale).background(seat.copy(alpha = 0.5f), CircleShape))
        GhostSlot(sizeDp)
    }
}

/**
 * The wake bloom: washed and desaturated while the plates are still being
 * counted, blooming to full presence when the bowl becomes the way forward.
 * One morph, never a swap; a bowl that comes alive reads as alive.
 */
@Composable
private fun Modifier.bowlWash(awake: Boolean): Modifier {
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
private fun Modifier.bowlTap(ready: Boolean, copy: Copy, onPour: () -> Unit): Modifier {
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
private fun PourCue(text: String, modifier: Modifier = Modifier) {
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
