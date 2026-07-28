package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.FreeState
import app.maqsadah.count_and_play.core.MAX_COUNT
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.core.inZone

/**
 * The free tray. A heap, a bowl, and no question.
 *
 * The bowl wears its count on the rim and the number changes as he plays, so
 * the quantity and its name move together in front of him — which is the whole
 * lesson the rest of the app is working towards, offered here with nothing at
 * stake and no way to be wrong.
 */
@Composable
fun FreeTrayScreen(
    state: FreeState,
    copy: Copy,
    modifier: Modifier = Modifier,
    onTap: (Int) -> Unit,
) {
    val ids = state.tokens.map { it.id }.toSet()
    Column(
        modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tray(
            tokens = state.tokens.inZone(Zone.SOURCE),
            capacity = MAX_COUNT,
            revealed = ids,
            highlighted = emptySet(),
            copy = copy,
            modifier = Modifier.weight(1f),
            label = copy.ui.freePlay,
            onTapToken = onTap,
        )
        Tray(
            tokens = state.tokens.inZone(Zone.BOWL),
            capacity = MAX_COUNT,
            revealed = ids,
            highlighted = emptySet(),
            copy = copy,
            modifier = Modifier.weight(1f),
            label = "${state.made} ${copy.noun(state.shape, state.made)}",
            showEmptySlots = true,
            // Live, and the only number on screen. It is a description of what
            // he has made, never a target he has to reach.
            cardinal = if (state.made > 0) copy.digits(state.made) else null,
            onTapToken = onTap,
        )
    }
}
