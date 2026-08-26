package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.Token

@Composable
fun AddScreen(
    state: AddState,
    copy: Copy,
    onTap: (Int) -> Unit,
    onPour: () -> Unit,
    onHome: () -> Unit,
) {
    // The title follows the phase: count the plates, pour, count the whole.
    val prompt = when {
        state.poured -> copy.promptAll()
        state.platesReady -> copy.promptAdd()
        else -> copy.promptCount()
    }
    ActivityFrame(prompt, copy, onHome) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sizes = solveAddTraySizes(
                playWidth = maxWidth,
                bigPlate = maxOf(state.a, state.b),
                total = state.a + state.b,
                availHeight = maxHeight,
            )
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SectionGap),
            ) {
                PlatesRow(state, copy, sizes, onTap)
                BowlColumn(state, copy, sizes, onPour, onTap, Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The bowl column, in every phase. Roomy screens hold the full-size bowl
 * from the first frame: ghost seats while he counts the plates, the wake
 * bloom when both are counted, pieces falling into those same seats on the
 * pour. Tight screens keep a slim sleeping strip in phase one and rise the
 * full bowl in with the pour, folding the plates to make its room.
 */
@Composable
private fun BowlColumn(
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
    }
}

/** The two plates side by side; the right one sleeps until the left is counted out. */
@Composable
private fun PlatesRow(state: AddState, copy: Copy, sizes: TraySizes, onTap: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(PlateGap)) {
        PlateTray(
            rim = Blue,
            tokens = state.plateA,
            objectSize = if (state.poured) sizes.plateAfter else sizes.plate,
            perRow = sizes.platePerRow,
            badge = if (state.doneA) copy.digits(state.a) else null,
            badgeCentred = state.poured,
            enabled = true,
            copy = copy,
            modifier = Modifier.weight(1f),
            onTap = onTap,
        )
        PlateTray(
            rim = Orange,
            tokens = state.plateB,
            objectSize = if (state.poured) sizes.plateAfter else sizes.plate,
            perRow = sizes.platePerRow,
            badge = if (state.doneB) copy.digits(state.b) else null,
            badgeCentred = state.poured,
            enabled = state.doneA || state.poured,
            copy = copy,
            modifier = Modifier.weight(1f),
            onTap = onTap,
        )
    }
}

/**
 * One plate of objects. A sleeping plate keeps its place and its rim colour
 * but its pieces are drawn washed-out and monochrome, so the child can see
 * which column is his before its turn arrives. A finished plate wears its
 * total as a popped-on badge that survives the pour.
 */
@Composable
private fun PlateTray(
    rim: Color,
    tokens: List<Token>,
    objectSize: Dp,
    perRow: Int,
    badge: String?,
    badgeCentred: Boolean,
    enabled: Boolean,
    copy: Copy,
    modifier: Modifier,
    onTap: (Int) -> Unit,
) {
    Box(modifier) {
        Tray(tokens.size, TraySolution(objectSize, perRow), Modifier.fillMaxWidth()) { size ->
            val washout = if (enabled) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    alpha = 0.4f
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(0f) })
                }
            }
            tokens.forEach { token ->
                key(token.id) {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.counted) copy.digits(token.countOrder) else null,
                        label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                        modifier = washout,
                        onTap = if (enabled) ({ onTap(token.id) }) else null,
                    )
                }
            }
        }
        if (badge != null) {
            // Standing plates pin their total to the bottom-right corner: the
            // one spot count chips never reach (they sit at objects' top-right).
            // Folded places hold the total centred.
            val alignment = if (badgeCentred) Alignment.Center else Alignment.BottomEnd
            TotalBadge(badge, rim, Modifier.align(alignment).padding(10.dp))
        }
    }
}

/** The plate's finished total: a candy disc with the numeral, popping onto the plate. */
@Composable
private fun TotalBadge(text: String, ring: Color, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.4f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && scale.value < 1f) scale.animateTo(1f, BadgeSpring)
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .size(56.dp)
            .shadow(elevation = LiftHeld, shape = CircleShape, clip = false)
            .background(Liner, CircleShape)
            .border(BorderStroke(4.dp, ring), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = flashTint(ring),
            fontSize = 28.sp,
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
    Tray(
        state.total,
        TraySolution(sizes.bowl, sizes.bowlPerRow),
        modifier.then(tap).then(wash),
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
}

/** An empty bowl seat: the part's colour under the dashed slot to come. */
@Composable
private fun GhostSeat(sizeDp: Dp, seat: Color) {
    Box(Modifier.size(sizeDp * SeatScale), contentAlignment = Alignment.Center) {
        Box(Modifier.size(sizeDp * 1.3f).background(seat.copy(alpha = 0.5f), CircleShape))
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

/*
 * The pour button is gone: the bowl below the plates IS the button now, so
 * the words ask from the headline and the destination receives the tap.
 */
