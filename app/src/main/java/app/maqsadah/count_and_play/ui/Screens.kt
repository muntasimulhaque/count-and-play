package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    // The chip says WHEN this one was counted: its position among the counted.
    val countedIds = state.tokens.filter { it.counted }.map { it.id }
    ActivityFrame(copy.promptCount(), onHome) {
        Tray(Blue, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                ObjectView(
                    shape = token.shape,
                    sizeDp = size,
                    chip = if (token.counted) copy.digits(countedIds.indexOf(token.id) + 1) else null,
                    onTap = { onTap(token.id) },
                )
            }
        }
    }
}

@Composable
fun AddScreen(state: AddState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    ActivityFrame(copy.promptAdd(), onHome) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Tray(Blue, state.plateA.size, Modifier.weight(1f)) { size ->
                    state.plateA.forEach { token ->
                        ObjectView(shape = token.shape, sizeDp = size, onTap = { onTap(token.id) })
                    }
                }
                Tray(Orange, state.plateB.size, Modifier.weight(1f)) { size ->
                    state.plateB.forEach { token ->
                        ObjectView(shape = token.shape, sizeDp = size, onTap = { onTap(token.id) })
                    }
                }
            }
            Tray(Green, state.bowl.size, Modifier.fillMaxWidth()) { size ->
                state.bowl.forEach { token ->
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        // Each part keeps its plate's colour under it in the bowl.
                        seat = if (token.origin == 1) SeatA else SeatB,
                    )
                }
            }
        }
    }
}

@Composable
fun TakeScreen(state: TakeState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    ActivityFrame(copy.promptTake(state.b), onHome) {
        Tray(Pink, state.n, Modifier.align(Alignment.Center)) { size ->
            state.tokens.forEach { token ->
                // A gone token keeps its slot as a dashed outline, so the row
                // still reads as "some of these are left".
                if (token.gone) {
                    GhostSlot(size)
                } else {
                    ObjectView(shape = token.shape, sizeDp = size, onTap = { onTap(token.id) })
                }
            }
        }
    }
}

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
private fun ActivityFrame(prompt: String, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                prompt,
                Modifier.fillMaxWidth().padding(top = 16.dp),
                textAlign = TextAlign.Center,
                color = Ink,
                fontSize = SizePrompt,
                fontWeight = ToyBold,
                fontFamily = ToyFont,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
        }
        HomeButton(Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp), onHome)
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
