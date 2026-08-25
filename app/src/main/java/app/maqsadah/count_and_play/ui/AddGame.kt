package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
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
                if (state.poured) {
                    // The bowl slides in beneath the unchanged plates; the key is gone.
                    BowlTray(state, copy, TraySolution(sizes.bowl, sizes.bowlPerRow), onTap)
                } else {
                    PourButton(
                        label = copy.promptAdd(),
                        enabled = state.platesReady,
                        notYetState = copy.pourNotYetState(),
                        onPour = onPour,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
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

private val BadgeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/** The darker fact-card variant of a tray rim, for legible numerals on white. */
private fun flashTint(rim: Color): Color = when (rim) {
    Blue -> FlashBlue
    Orange -> FlashOrange
    Pink -> FlashPink
    else -> Ink
}

@Composable
private fun BowlTray(state: AddState, copy: Copy, layout: TraySolution, onTap: (Int) -> Unit) {
    Tray(state.bowl.size, layout, Modifier.fillMaxWidth()) { size ->
        state.bowl.forEach { token ->
            key(token.id) {
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

/**
 * The pour button: the one big yellow key of the game. Asleep (washed out,
 * untappable) until both plates are counted; then it wakes as a pressable
 * key sized to its words, centred, waiting for the child's finger.
 */
@Composable
private fun PourButton(
    label: String,
    enabled: Boolean,
    notYetState: String,
    onPour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = @Composable { awake: Boolean ->
        Text(
            label,
            Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            color = if (awake) Ink else Ink.copy(alpha = 0.55f),
            fontSize = SizeLabel,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
    if (enabled) {
        Keycap(
            edge = YellowEdge,
            modifier = modifier,
            edgeHeight = 8.dp,
            stretch = false,
            onClick = onPour,
            fill = Yellow,
        ) { text(true) }
    } else {
        Box(
            modifier
                .padding(top = 8.dp)
                .background(Ink.copy(alpha = 0.06f), RoundedCornerShape(Corner))
                .border(
                    BorderStroke(1.dp, Hairline),
                    RoundedCornerShape(Corner),
                )
                .semantics {
                    role = Role.Button
                    stateDescription = notYetState
                },
            contentAlignment = Alignment.Center,
        ) { text(false) }
    }
}
