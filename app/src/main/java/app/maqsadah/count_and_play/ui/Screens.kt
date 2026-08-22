package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.CountState
import app.maqsadah.count_and_play.core.TakeState
import app.maqsadah.count_and_play.core.Token

@Composable
fun CountScreen(state: CountState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    // The chip says WHEN this one was counted: the child's own tap order.
    ActivityFrame(copy.promptCount(), copy, onHome) {
        Tray(Blue, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                key(token.id) {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.counted) copy.digits(token.countOrder) else null,
                        label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                        onTap = { onTap(token.id) },
                    )
                }
            }
        }
    }
}

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
        // Both trays are sized together against the room this box actually
        // has, in both phases: plates counted with an empty bowl waiting, and
        // the poured bowl counted with the emptied plates keeping their place.
        // The sizes come from the round's numbers alone, so nothing jumps when
        // the plates pour, and even 5 + 5 with a full ten-token bowl fits.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sizes = solveAddTraySizes(
                playWidth = maxWidth,
                bigPlate = maxOf(state.a, state.b),
                total = state.a + state.b,
                availHeight = maxHeight,
            )
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                PlatesRow(state, copy, sizes.plate, onTap)
                PourButton(
                    label = copy.promptAdd(),
                    enabled = state.platesReady && !state.poured,
                    visible = !state.poured,
                    readyState = copy.pourReadyState(),
                    notYetState = copy.pourNotYetState(),
                    onPour = onPour,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                BowlTray(state, copy, sizes.bowl, onTap)
            }
        }
    }
}

/** The two plates side by side, each counted on its own. */
@Composable
private fun PlatesRow(state: AddState, copy: Copy, objectSize: Dp, onTap: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(PlateGap)) {
        PlateTray(Blue, state.plateA, copy, objectSize, Modifier.weight(1f), onTap)
        PlateTray(Orange, state.plateB, copy, objectSize, Modifier.weight(1f), onTap)
    }
}

@Composable
private fun PlateTray(
    rim: Color,
    tokens: List<Token>,
    copy: Copy,
    objectSize: Dp,
    modifier: Modifier,
    onTap: (Int) -> Unit,
) {
    Tray(rim, tokens.size, modifier, objectSize = objectSize) { size ->
        tokens.forEach { token ->
            key(token.id) {
                ObjectView(
                    shape = token.shape,
                    sizeDp = size,
                    chip = if (token.counted) copy.digits(token.countOrder) else null,
                    label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                    onTap = { onTap(token.id) },
                )
            }
        }
    }
}

@Composable
private fun BowlTray(state: AddState, copy: Copy, objectSize: Dp, onTap: (Int) -> Unit) {
    Tray(Green, state.bowl.size, Modifier.fillMaxWidth(), objectSize = objectSize) { size ->
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
 * The pour button. Asleep (washed out, untappable) until both plates are
 * counted; then it wakes in full candy and waits for the child's finger.
 * It stays composed after the pour as invisible reserved space, so the bowl
 * never jumps when the button leaves.
 */
@Composable
private fun PourButton(
    label: String,
    enabled: Boolean,
    visible: Boolean,
    readyState: String,
    notYetState: String,
    onPour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = modifier
        .background(if (enabled) Yellow else Ink.copy(alpha = 0.10f), RoundedCornerShape(Corner))
        .border(
            BorderStroke(OutlineWidth, if (enabled) Orange else Ink.copy(alpha = 0.22f)),
            RoundedCornerShape(Corner),
        )
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
        ) { onPour() }
        .padding(horizontal = 28.dp, vertical = 12.dp)
    Box(
        if (visible) {
            base
                .semantics {
                    role = Role.Button
                    stateDescription = if (enabled) readyState else notYetState
                }
        } else {
            // Invisible spacer with no semantics: nothing for a screen reader
            // to land on, but the layout keeps its height.
            base.graphicsLayer { alpha = 0f }.clearAndSetSemantics { }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Ink else Ink.copy(alpha = 0.55f),
            fontSize = SizeLabel,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

@Composable
fun TakeScreen(state: TakeState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    // Once the asked number is out, the question becomes "how many are left?".
    val prompt = if (state.removalDone) copy.promptLeft() else copy.promptTake(state.b)
    ActivityFrame(prompt, copy, onHome) {
        Tray(Pink, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                // One node per slot across the whole round: a taken token sinks
                // into its ghost inside the very same composable that was
                // tappable, so focus and identity never reset mid-round.
                key(token.id) {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        gone = token.gone,
                        chip = if (token.countOrder > 0) copy.digits(token.countOrder) else null,
                        label = copy.objectLabel(token.shape.name, token.countOrder),
                        onTap = if (token.gone) null else ({ onTap(token.id) }),
                    )
                }
            }
        }
    }
}

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
private fun ActivityFrame(prompt: String, copy: Copy, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeButton(copy.homeLabel(), onHome)
            Text(
                prompt,
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Ink,
                fontSize = SizePrompt,
                fontWeight = ToyBold,
                fontFamily = ToyFont,
            )
            // Mirrors the home button so the prompt stays optically centred and
            // can never slide underneath it, whatever the screen width or font.
            Spacer(Modifier.width(52.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun HomeButton(description: String, onHome: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onHome() }
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        HouseIcon(30.dp, Ink)
    }
}

/** A plain house: roof, wall, door. Vector, static, no emoji. */
@Composable
private fun HouseIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val roof = Path().apply {
            moveTo(w * 0.04f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.96f, h * 0.48f)
            close()
        }
        drawPath(roof, color)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.16f, h * 0.44f),
            size = Size(w * 0.68f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.08f),
        )
        drawRoundRect(
            Liner,
            topLeft = Offset(w * 0.40f, h * 0.62f),
            size = Size(w * 0.20f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
    }
}
