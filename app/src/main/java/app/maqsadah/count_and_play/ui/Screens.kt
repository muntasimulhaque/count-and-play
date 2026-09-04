package app.maqsadah.count_and_play.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
            Tray(state.n, layout, Modifier.align(Alignment.Center)) { size ->
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
    // The ask follows the phases: count the whole tray, take b away, count
    // what is left. The subtraction ask waits until the whole is counted.
    val prompt = when {
        !state.totalDone -> copy.promptCount()
        state.removalDone -> copy.promptLeft()
        else -> copy.promptTake(state.b)
    }
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

/** The ask itself, in numerals: 5 − 1 hangs above the tray it describes. It
 *  holds its place invisibly until the whole has been counted, then fades up:
 *  the layout never jumps, and the symbols arrive only with the act they
 *  name, never before the child has counted what they are about to act on. */
@Composable
private fun TakeEquation(state: TakeState, copy: Copy, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val shown by animateFloatAsState(
        targetValue = if (state.totalDone) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 260),
        label = "takeEq",
    )
    Row(
        modifier
            .graphicsLayer { alpha = shown }
            .then(if (state.totalDone) Modifier else Modifier.clearAndSetSemantics { }),
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
    Tray(state.n, TraySolution(solution.size, solution.mainPerRow), Modifier.fillMaxWidth()) { size ->
        state.tokens.forEach { token ->
            key(token.id) {
                if (token.gone) {
                    // The ghost's node must be the node the object occupied,
                    // so rows keep one rhythm after the taking.
                    GhostSlot(size, nodeOf(size, seated = false))
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
    Tray(
        state.removed,
        TraySolution(solution.size, solution.takenPerRow),
        Modifier.fillMaxWidth(),
        tint = Purple,
    ) { size ->
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

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
internal fun ActivityFrame(prompt: String, copy: Copy, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeButton(copy.homeLabel(), onHome)
            // The ask cross-fades when the game's phase rewrites it, so the
            // words trade places gently instead of hard-swapping mid-play.
            AnimatedContent(
                targetState = prompt,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        (
                            fadeIn(tween(durationMillis = 180)) +
                                slideInVertically(tween(durationMillis = 180)) { it / 3 }
                            ) togetherWith fadeOut(tween(durationMillis = 120))
                    }
                },
                label = "prompt",
                modifier = Modifier.weight(1f),
            ) { text ->
                Text(
                    text,
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Ink,
                    fontSize = SizePrompt,
                    fontWeight = ToyBold,
                    fontFamily = ToyFont,
                )
            }
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
            .pressable(onClick = onHome)
            .background(Liner, CircleShape)
            .border(BorderStroke(1.dp, Hairline), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        HouseIcon(28.dp, Ink.copy(alpha = 0.85f))
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
