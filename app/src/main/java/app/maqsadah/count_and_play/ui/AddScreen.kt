package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState

/** ADD: count the left plate, count the right, pour, count the whole. */
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
            badgeCentered = state.poured,
            awake = true,
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
            badgeCentered = state.poured,
            awake = state.doneA || state.poured,
            copy = copy,
            modifier = Modifier.weight(1f),
            onTap = onTap,
        )
    }
}
