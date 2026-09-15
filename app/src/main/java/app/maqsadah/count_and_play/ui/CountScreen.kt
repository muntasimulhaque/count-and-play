package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.CountState

/** COUNT: one tray, tapped in any order, each tap leaving its numbered chip. */
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
