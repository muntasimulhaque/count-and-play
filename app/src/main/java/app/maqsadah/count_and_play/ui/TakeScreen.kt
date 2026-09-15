package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.TakeState

/** TAKE: count the whole tray, tap the ask away, then count what is left. */
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

/**
 * The ask itself, in numerals: 5 − 1 hangs above the tray it describes. It
 * holds its place invisibly until the whole has been counted, then fades up:
 * the layout never jumps, and the symbols arrive only with the act they
 * name, never before the child has counted what they are about to act on.
 */
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
            .equationSemantics(state, copy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AskNumeral(copy.digits(state.n), FlashBlue)
        AskOperator("\u2212")
        AskNumeral(copy.digits(state.b), FlashPink)
    }
}

/**
 * One spoken fact for a screen reader: the numerals as a subtraction, not
 * three bare glyph nodes. Before the whole is counted the ask keeps no
 * semantics at all, so nothing announces a fact the child has not made yet.
 */
private fun Modifier.equationSemantics(state: TakeState, copy: Copy): Modifier =
    if (state.totalDone) {
        semantics(mergeDescendants = true) {
            contentDescription = "${copy.digits(state.n)} \u2212 ${copy.digits(state.b)}"
        }
    } else {
        clearAndSetSemantics { }
    }

/** One numeral of the hanging ask, in the fact-card variant of its hue. */
@Composable
private fun AskNumeral(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = SizeEquation,
        lineHeight = EquationLine.value.sp,
        fontWeight = ToyBlack,
        fontFamily = ToyFont,
    )
}

/** The operator between the ask's two numerals. */
@Composable
private fun AskOperator(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 10.dp),
        color = Ink,
        fontSize = SizeEquation * 0.62f,
        lineHeight = EquationLine.value.sp * 0.62f,
        fontWeight = ToyBlack,
        fontFamily = ToyFont,
    )
}

/**
 * The whole bowl of n slots. A taken token leaves a dashed ghost behind, so
 * the original five still reads as five; what is gone is visible below.
 */
@Composable
private fun MainTray(state: TakeState, copy: Copy, solution: TakeSolution, onTap: (Int) -> Unit) {
    Tray(state.n, TraySolution(solution.size, solution.mainPerRow), Modifier.fillMaxWidth()) { size ->
        state.tokens.forEach { token ->
            key(token.id) {
                // One node per slot across the taking: the piece sinks into
                // its ghost instead of blinking out, and the empty slot stays
                // reachable (a tap on it is heard and recorded, never dead).
                ObjectView(
                    shape = token.shape,
                    sizeDp = size,
                    chip = if (token.countOrder > 0) copy.digits(token.countOrder) else null,
                    gone = token.gone,
                    label = if (token.gone) null else copy.objectLabel(token.shape.name, token.countOrder),
                    onTap = { onTap(token.id) },
                )
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
        // Pink is the take-away hue everywhere else (the equation's subtrahend,
        // the shelf's miniature), so the box the pieces land in wears it too.
        tint = Pink,
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
