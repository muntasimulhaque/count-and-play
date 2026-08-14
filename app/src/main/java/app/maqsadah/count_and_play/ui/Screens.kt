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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.CountState
import app.maqsadah.count_and_play.core.TakeState

@Composable
fun CountScreen(state: CountState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    // The chip says WHEN this one was counted: the child's own tap order.
    ActivityFrame(copy.promptCount(), onHome) {
        Tray(Blue, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                ObjectView(
                    shape = token.shape,
                    sizeDp = size,
                    chip = if (token.counted) copy.digits(token.countOrder) else null,
                    onTap = { onTap(token.id) },
                )
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
    ActivityFrame(prompt, onHome) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Tray(Blue, state.plateA.size, Modifier.weight(1f)) { size ->
                    state.plateA.forEach { token ->
                        ObjectView(
                            shape = token.shape,
                            sizeDp = size,
                            chip = if (token.counted) copy.digits(token.countOrder) else null,
                            onTap = { onTap(token.id) },
                        )
                    }
                }
                Tray(Orange, state.plateB.size, Modifier.weight(1f)) { size ->
                    state.plateB.forEach { token ->
                        ObjectView(
                            shape = token.shape,
                            sizeDp = size,
                            chip = if (token.counted) copy.digits(token.countOrder) else null,
                            onTap = { onTap(token.id) },
                        )
                    }
                }
            }
            if (!state.poured) {
                PourButton(
                    label = copy.promptAdd(),
                    ready = state.platesReady,
                    onPour = onPour,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Tray(Green, state.bowl.size, Modifier.fillMaxWidth()) { size ->
                state.bowl.forEach { token ->
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.counted) copy.digits(token.countOrder) else null,
                        // Each part keeps its plate's colour under it in the bowl.
                        seat = if (token.origin == 1) SeatA else SeatB,
                        onTap = { onTap(token.id) },
                    )
                }
            }
        }
    }
}

/**
 * The pour button. Asleep (washed out, untappable) until both plates are
 * counted; then it wakes in full candy and waits for the child's finger.
 */
@Composable
private fun PourButton(label: String, ready: Boolean, onPour: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(if (ready) Yellow else Ink.copy(alpha = 0.08f), RoundedCornerShape(Corner))
            .border(
                BorderStroke(OutlineWidth, if (ready) Orange else Ink.copy(alpha = 0.15f)),
                RoundedCornerShape(Corner),
            )
            .then(
                if (ready) {
                    Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { onPour() }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (ready) Ink else Ink.copy(alpha = 0.35f),
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
    ActivityFrame(prompt, onHome) {
        Tray(Pink, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                // A taken token sinks into its ghost wearing its take-away
                // number; a leftover can be tapped to count it.
                if (token.gone) {
                    TakenSlot(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.countOrder > 0) copy.digits(token.countOrder) else null,
                    )
                } else {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.counted) copy.digits(token.countOrder) else null,
                        onTap = { onTap(token.id) },
                    )
                }
            }
        }
    }
}

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
private fun ActivityFrame(prompt: String, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeButton(Modifier, onHome)
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
            // can never slide underneath it — whatever the screen width or font.
            Spacer(Modifier.width(52.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun HomeButton(modifier: Modifier, onHome: () -> Unit) {
    Box(
        modifier.size(52.dp).clickable(remember { MutableInteractionSource() }, indication = null) { onHome() },
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
