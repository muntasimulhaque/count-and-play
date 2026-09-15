package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.core.ShapeKind

// ---- The three miniatures: each tile shows the game it opens, big enough
// ---- to be read from across a room. Each scene sizes itself from the room
// ---- its tile grants: a share of the room for the one-row scenes (COUNT,
// ---- TAKE), a two-row budget for ADD, all capped so the smallest phone and
// ---- the largest tablet both stay clear of the label and each other.

@Composable
private fun MiniShape(kind: ShapeKind, sizeDp: Dp) {
    Canvas(Modifier.size(sizeDp)) { drawCountable(kind, size.minDimension) }
}

/**
 * A soft wash of the game's hue instead of a bordered box: the grouping
 * reads at a glance while the chrome stays out of the picture's way.
 */
@Composable
private fun MiniPanel(rim: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(rim.copy(alpha = 0.08f), RoundedCornerShape(CornerSmall))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MiniSeated(kind: ShapeKind, seat: Color, seatSize: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(seatSize).background(seat, CircleShape))
        MiniShape(kind, seatSize * 0.74f)
    }
}

@Composable
internal fun CountMini(room: Dp) {
    MiniPanel(Blue) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { MiniShape(ShapeKind.APPLE, miniUnit(room)) }
        }
    }
}

@Composable
internal fun AddMini(room: Dp) {
    // The plate row and the bowl row share the room: two rows and the gap
    // between them decide the seat, and the seat decides the piece. Seats a
    // touch larger than the loose shapes, the same part-color story the game
    // itself tells.
    val seat = ((room - 8.dp) / 2).coerceIn(36.dp, 120.dp)
    val shape = (seat * 0.72f).coerceIn(26.dp, 88.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniPanel(Blue) { MiniShape(ShapeKind.STAR, shape) }
            MiniPanel(Orange) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniShape(ShapeKind.BALL, shape)
                    MiniShape(ShapeKind.BALL, shape)
                }
            }
        }
        MiniPanel(Green) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniSeated(ShapeKind.STAR, SeatA, seat)
                MiniSeated(ShapeKind.BALL, SeatB, seat)
                MiniSeated(ShapeKind.BALL, SeatB, seat)
            }
        }
    }
}

@Composable
internal fun TakeMini(room: Dp) {
    MiniPanel(Pink) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniShape(ShapeKind.APPLE, miniUnit(room))
            GhostSlot(miniUnit(room))
            GhostSlot(miniUnit(room))
        }
    }
}

/** The one-row scenes' shape size: a third of the tile's spare room, capped. */
private fun miniUnit(room: Dp): Dp = (room * 0.34f).coerceIn(34.dp, 96.dp)
