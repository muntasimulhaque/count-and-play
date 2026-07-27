package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.LessonState
import app.maqsadah.count_and_play.core.RESERVE_SIZE
import app.maqsadah.count_and_play.core.Step
import app.maqsadah.count_and_play.core.Task
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.core.inZone
import app.maqsadah.count_and_play.host.Fx

/**
 * Arranges the trays.
 *
 * Addition and subtraction use the same furniture in opposite directions: two
 * dishes pour into one bowl, one bowl pours into a dish. A child who plays both
 * meets one structure, not two — which is also why part-whole needs no new
 * visual vocabulary later.
 */
@Composable
fun Stage(
    state: LessonState,
    fx: Fx,
    copy: Copy,
    modifier: Modifier = Modifier,
    onTapToken: (Int) -> Unit,
    onTapZone: (Zone) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val task = state.task) {
            // A five-frame, always: one object shown in a frame of five is
            // "one, and four empty", which is the whole reason the frame exists.
            is Task.CountIt ->
                TrayFor(Zone.BOWL, frame(task.n), state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone, empties = true)

            is Task.GiveMe -> {
                TrayFor(Zone.SOURCE, task.pool, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
                TrayFor(Zone.BOWL, FRAME_COLS, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone, empties = true)
            }

            is Task.WhichIsMore -> Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrayFor(Zone.DISH_A, task.left, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
                TrayFor(Zone.DISH_B, task.right, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
            }

            is Task.UnderTheLeaf -> {
                Box(Modifier.weight(1f)) {
                    TrayFor(Zone.BOWL, FRAME_COLS, state, fx, copy, Modifier.fillMaxSize(), onTapToken, onTapZone, empties = true)
                    // The leaf hides the set without destroying it — the whole
                    // point is that the objects are obviously still there.
                    if (Zone.BOWL in fx.covered) {
                        Leaf(Modifier.fillMaxSize())
                    }
                }
                if (state.tokens.inZone(Zone.DISH_B).isNotEmpty()) {
                    TrayFor(Zone.DISH_B, 2, state, fx, copy, Modifier.weight(0.6f), onTapToken, onTapZone)
                }
            }

            is Task.Join -> {
                if (state.tokens.inZone(Zone.BOWL).isEmpty()) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrayFor(Zone.DISH_A, task.a, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
                        TrayFor(Zone.DISH_B, task.b, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
                    }
                } else {
                    TrayFor(Zone.BOWL, FRAME_COLS, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone, empties = true)
                }
            }

            is Task.Separate -> Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrayFor(Zone.BOWL, FRAME_COLS, state, fx, copy, Modifier.weight(1.4f), onTapToken, onTapZone, empties = true)
                // What left is still somewhere, whole and countable.
                TrayFor(Zone.DISH_B, task.take, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone, empties = true)
            }
        }

        if (fx.predicting) {
            Row(
                Modifier.weight(0.9f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnswerFrame(state, fx, copy, Modifier.weight(1f), onTapToken)
                TrayFor(Zone.RESERVE, RESERVE_SIZE, state, fx, copy, Modifier.weight(1f), onTapToken, onTapZone)
            }
        }
    }
}

@Composable
private fun TrayFor(
    zone: Zone,
    capacity: Int,
    state: LessonState,
    fx: Fx,
    copy: Copy,
    modifier: Modifier,
    onTapToken: (Int) -> Unit,
    onTapZone: (Zone) -> Unit,
    empties: Boolean = false,
) {
    Tray(
        tokens = state.tokens.inZone(zone),
        capacity = capacity,
        revealed = fx.revealed,
        highlighted = fx.highlighted,
        copy = copy,
        modifier = modifier,
        label = zoneLabel(zone, state, copy),
        showEmptySlots = empties,
        // The many tags becoming one number is the count-to-cardinal
        // transition — the step children actually fail.
        cardinal = fx.cardinals[zone]?.let(copy::digits),
        onTapToken = onTapToken,
        onTapEmpty = { onTapZone(zone) },
    )
}

@Composable
private fun AnswerFrame(
    state: LessonState,
    fx: Fx,
    copy: Copy,
    modifier: Modifier,
    onTapToken: (Int) -> Unit,
) {
    val palette = LocalPalette.current
    // One surface, not a tray nested inside a frame — the two backgrounds used
    // to stack and render as a band across the middle of the answer box.
    Tray(
        tokens = state.tokens.inZone(Zone.ANSWER),
        capacity = maxOf(state.tokens.inZone(Zone.ANSWER).size, FRAME_COLS),
        revealed = fx.revealed + state.tokens.inZone(Zone.ANSWER).map { it.id },
        highlighted = fx.highlighted,
        copy = copy,
        modifier = modifier,
        label = "answer",
        showEmptySlots = true,
        surface = palette.trayLiner,
        rim = palette.answerFrame,
        onTapToken = onTapToken,
    )
}

/** A plain leaf. A cover, not a creature. */
@Composable
private fun Leaf(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .padding(6.dp)
            .background(palette.leaf, RoundedCornerShape(topStart = 90.dp, bottomEnd = 90.dp))
            .border(4.dp, palette.leafStroke, RoundedCornerShape(topStart = 90.dp, bottomEnd = 90.dp)),
    )
}

/** Counting practice may run to ten; arithmetic never leaves the five-frame. */
private fun frame(n: Int) = if (n <= FRAME_COLS) FRAME_COLS else 2 * FRAME_COLS

private fun zoneLabel(zone: Zone, state: LessonState, copy: Copy): String {
    val count = state.tokens.inZone(zone).size
    val counted = state.tokens.inZone(zone).count { it.isCounted }
    val name = copy.noun(state.task.shape, count)
    // TalkBack hears the quantity and how much of it is done, which is what the
    // screen actually means.
    return "$count $name, $counted counted"
}
