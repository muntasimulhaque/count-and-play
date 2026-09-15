package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.Token

/**
 * The bowl column, in every phase. Roomy screens hold the full-size bowl
 * from the first frame: ghost seats while he counts the plates, the wake
 * bloom when both are counted, pieces falling into those same seats on the
 * pour. Tight screens keep a slim sleeping strip in phase one and rise the
 * full bowl in with the pour, folding the plates to make its room.
 */
@Composable
internal fun BowlColumn(
    state: AddState,
    copy: Copy,
    sizes: TraySizes,
    onPour: () -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    when {
        !sizes.bowlInPlace && !state.poured ->
            BowlStrip(state, copy, onPour, modifier)
        !sizes.bowlInPlace && state.poured ->
            RiseIn { BowlTray(state, copy, TraySolution(sizes.bowl, sizes.bowlPerRow), onTap) }
        else ->
            BowlInPlace(state, copy, sizes, onPour, onTap, modifier)
    }
}

/**
 * The tight screen's sleeping bowl: a slim well strip with a dashed mouth,
 * washed out until both plates are counted, then awake. A tap answers the
 * finger at any time: the soft tick before it is time, the pour after.
 */
@Composable
private fun BowlStrip(state: AddState, copy: Copy, onPour: () -> Unit, modifier: Modifier) {
    val ready = state.platesReady
    val wash = Modifier.bowlWash(ready)
    WellSurface(
        tint = Green,
        modifier = modifier
            .heightIn(min = BowlAsleepReserve)
            .bowlTap(ready, copy, onPour)
            .then(wash),
    ) {
        GhostSlot(44.dp)
        // When it wakes, the destination says what it is for, right where
        // the finger goes: a cue that survives the sound switch.
        if (ready) PourCue(copy.promptAdd(), Modifier.align(Alignment.Center))
    }
}

/** The bowl the pieces fell into, after the pour on a tight screen. */
@Composable
private fun BowlTray(state: AddState, copy: Copy, layout: TraySolution, onTap: (Int) -> Unit) {
    Tray(state.bowl.size, layout, Modifier.fillMaxWidth(), tint = Green) { size ->
        state.bowl.forEachIndexed { index, token -> BowlPiece(token, index, size, copy, onTap) }
    }
}

/**
 * The roomy screen's bowl, present from the first frame and constant through
 * the pour: before it, total ghost seats on their part colors, washed out
 * until both plates are counted and the whole bowl is the tap target; after
 * it, the pieces fallen into exactly those seats, each one tappable for the
 * fresh count of the whole. The geometry never changes, so the pour moves
 * pieces, not furniture.
 */
@Composable
private fun BowlInPlace(
    state: AddState,
    copy: Copy,
    sizes: TraySizes,
    onPour: () -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    val wash = if (state.poured) Modifier else Modifier.bowlWash(state.platesReady)
    val tap = if (state.poured) {
        Modifier
    } else {
        Modifier.bowlTap(state.platesReady, copy, onPour)
    }
    Box(modifier.then(tap).then(wash)) {
        Tray(
            state.total,
            TraySolution(sizes.bowl, sizes.bowlPerRow),
            Modifier.fillMaxWidth(),
            // The bowl's own hue, matching the sleeping strip and the shelf's
            // miniature: one color says this is where the parts become whole.
            tint = Green,
        ) { size ->
            BowlSeats(state, copy, size, onTap)
        }
        // The awake bowl wears the ask itself, where the finger goes.
        if (state.platesReady && !state.poured) PourCue(copy.promptAdd(), Modifier.align(Alignment.Center))
    }
}

/** The bowl's seats: the fallen pieces after the pour, their ghosts before it. */
@Composable
private fun BowlSeats(state: AddState, copy: Copy, size: Dp, onTap: (Int) -> Unit) {
    if (state.poured) {
        state.bowl.forEachIndexed { index, token -> BowlPiece(token, index, size, copy, onTap) }
    } else {
        repeat(state.a) { GhostSeat(size, SeatA) }
        repeat(state.b) { GhostSeat(size, SeatB) }
    }
}

/**
 * One piece in the bowl, seated on its own plate's color and landing with the
 * pour's cascade, so five keeps reading as three-and-two at a glance.
 */
@Composable
private fun BowlPiece(token: Token, index: Int, size: Dp, copy: Copy, onTap: (Int) -> Unit) {
    key(token.id) {
        FallIn(index) {
            ObjectView(
                shape = token.shape,
                sizeDp = size,
                chip = if (token.counted) copy.digits(token.countOrder) else null,
                // Each part keeps its plate's color under it in the bowl.
                seat = if (token.origin == 1) SeatA else SeatB,
                label = copy.objectLabel(token.shape.name, if (token.counted) token.countOrder else 0),
                onTap = { onTap(token.id) },
            )
        }
    }
}

/** An empty bowl seat: the part's color under the dashed slot to come. The
 *  node matches the seat a fallen piece occupies, so the pour moves pieces,
 *  never furniture, and both phases of the bowl lay out on one rhythm. */
@Composable
private fun GhostSeat(sizeDp: Dp, seat: Color) {
    Box(Modifier.size(nodeOf(sizeDp, seated = true)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(sizeDp * SeatScale).background(seat.copy(alpha = 0.5f), CircleShape))
        GhostSlot(sizeDp)
    }
}
