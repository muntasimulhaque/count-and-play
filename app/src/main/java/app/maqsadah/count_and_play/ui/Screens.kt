package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.CountState
import app.maqsadah.count_and_play.core.TakeState

@Composable
fun CountScreen(state: CountState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    // The chip says WHEN this one was counted: the child's own tap order.
    ActivityFrame(copy.promptCount(), copy, onHome) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = solveTray(maxWidth, state.n, SingleCap, maxHeight)
            Tray(Blue, state.n, layout, Modifier.align(Alignment.Center)) { size ->
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
}

@Composable
fun TakeScreen(state: TakeState, copy: Copy, onTap: (Int) -> Unit, onHome: () -> Unit) {
    // Once the asked number is out, the question becomes "how many are left?".
    val prompt = if (state.removalDone) copy.promptLeft() else copy.promptTake(state.b)
    ActivityFrame(prompt, copy, onHome) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val solution = solveTakeSizes(maxWidth, state.n, state.removed, maxHeight)
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SectionGap),
            ) {
                TakeEquation(state, copy, Modifier.align(Alignment.CenterHorizontally))
                MainTray(state, copy, solution, onTap)
                TakenTray(state, copy, solution)
            }
        }
    }
}

/** The ask itself, in numerals: 5 − 1 hangs above the tray it describes. */
@Composable
private fun TakeEquation(state: TakeState, copy: Copy, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            copy.digits(state.n),
            color = FlashBlue,
            fontSize = SizeEquation,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
        Text(
            "\u2212",
            Modifier.padding(horizontal = 10.dp),
            color = Ink,
            fontSize = SizeEquation * 0.62f,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
        Text(
            copy.digits(state.b),
            color = FlashPink,
            fontSize = SizeEquation,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

private val SizeEquation = 40.sp

/**
 * The whole bowl of n slots. A taken token leaves a dashed ghost behind, so
 * the original five still reads as five; what is gone is visible below.
 */
@Composable
private fun MainTray(state: TakeState, copy: Copy, solution: TakeSolution, onTap: (Int) -> Unit) {
    Tray(Pink, state.n, TraySolution(solution.size, solution.mainPerRow), Modifier.fillMaxWidth()) { size ->
        state.tokens.forEach { token ->
            key(token.id) {
                if (token.gone) {
                    GhostSlot(size)
                } else {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = if (token.countOrder > 0) copy.digits(token.countOrder) else null,
                        label = copy.objectLabel(token.shape.name, token.countOrder),
                        onTap = { onTap(token.id) },
                    )
                }
            }
        }
    }
}

/** The taken-away box: empty at first, then one taken piece pops in per tap, wearing its number. */
@Composable
private fun TakenTray(state: TakeState, copy: Copy, solution: TakeSolution) {
    Tray(Purple, state.removed, TraySolution(solution.size, solution.takenPerRow), Modifier.fillMaxWidth()) { size ->
        state.tokens.filter { it.gone }.forEach { token ->
            key(token.id) {
                PopIn {
                    ObjectView(
                        shape = token.shape,
                        sizeDp = size,
                        chip = copy.digits(token.countOrder),
                        label = copy.objectLabel(token.shape.name, token.countOrder),
                    )
                }
            }
        }
    }
}

/** A taken piece lands in the lower box with a springy pop; reduced motion snaps. */
@Composable
private fun PopIn(content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.3f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && scale.value < 1f) scale.animateTo(1f, PopInSpring)
    }
    Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) { content() }
}

private val PopInSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
internal fun ActivityFrame(prompt: String, copy: Copy, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
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
