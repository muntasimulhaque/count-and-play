package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

/** The home shelf: the question on top, three enormous toy keys below. */
@Composable
fun HomeScreen(copy: Copy, onChoose: (Skill) -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mirrors the gear so the title stays optically centred and can
            // never slide underneath it, whatever the screen width or font.
            Spacer(Modifier.width(48.dp))
            FitTitle(copy.homeTitle(), Modifier.weight(1f))
            GearButton(Modifier, copy.settingsLabel(), onOpenSettings)
        }
        Tile(Skill.COUNT, copy.tileCount(), Modifier.weight(1f), onChoose) { room -> CountMini(room) }
        Tile(Skill.ADD, copy.tileAdd(), Modifier.weight(1f), onChoose) { room -> AddMini(room) }
        Tile(Skill.TAKE, copy.tileTake(), Modifier.weight(1f), onChoose) { room -> TakeMini(room) }
    }
}

/**
 * The shelf question on one line, whatever the language or the width: it
 * steps its size down until it fits, so Bengali and English both stay a
 * single calm question instead of an awkward two-line wrap.
 */
@Composable
private fun FitTitle(text: String, modifier: Modifier) {
    var sizeSp by remember(text) { mutableFloatStateOf(SizeTitle.value) }
    Text(
        text,
        modifier,
        textAlign = TextAlign.Center,
        color = Ink,
        fontSize = sizeSp.sp,
        fontWeight = ToyBlack,
        fontFamily = ToyFont,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.didOverflowWidth && sizeSp > 18f) sizeSp -= 2f },
    )
}

/** One toy key: a white cap on the neutral sand edge, its scene centred inside. */
@Composable
private fun Tile(
    skill: Skill,
    label: String,
    modifier: Modifier,
    onChoose: (Skill) -> Unit,
    mini: @Composable (Dp) -> Unit,
) {
    Keycap(
        edge = EdgeNeutral,
        modifier = modifier.fillMaxWidth().padding(vertical = 7.dp),
        description = label,
        onClick = { onChoose(skill) },
    ) {
        // The scene rides a touch high so no label can ever collide with it
        // (the ADD miniature is two rows tall on the tightest screens). The
        // room left after that reserve drives the scene's size, so the
        // pictures grow with the tile instead of floating as postage stamps.
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(bottom = LabelReserve),
            contentAlignment = Alignment.Center,
        ) { mini(maxHeight) }
        Text(
            label,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            color = Ink,
            fontSize = SizeLabel,
            // A tight line box, so the reserve above is honest at the capped
            // font scale instead of Baloo's near-double leading.
            lineHeight = SizeLabel * 1.15f,
            fontWeight = ToyBold,
            fontFamily = ToyFont,
            maxLines = 1,
        )
    }
}

/** The strip under every tile's scene that the label owns. */
private val LabelReserve = 46.dp

@Composable
private fun GearButton(modifier: Modifier, description: String, onOpenSettings: () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .pressable(onClick = onOpenSettings)
            .background(Liner, CircleShape)
            .border(BorderStroke(1.dp, Hairline), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(25.dp, Ink.copy(alpha = 0.65f))
    }
}

/** A plain gear: a thick hub ring, eight short teeth, a real hole. Vector, static, no emoji. */
@Composable
private fun GearIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        // The hub: one thick ring, so the centre stays a hole rather than a
        // disc. Teeth are short and fat, overlapping the hub's outer edge;
        // thin, long spokes read as a sun, which is what this used to look
        // like next to a settings label.
        drawCircle(color, radius = r * 0.50f, style = Stroke(width = r * 0.40f))
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat()
            val dy = kotlin.math.sin(a).toFloat()
            drawLine(
                color,
                Offset(center.x + dx * r * 0.62f, center.y + dy * r * 0.62f),
                Offset(center.x + dx * r * 0.94f, center.y + dy * r * 0.94f),
                strokeWidth = r * 0.34f,
                cap = StrokeCap.Round,
            )
        }
    }
}

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
private fun CountMini(room: Dp) {
    MiniPanel(Blue) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { MiniShape(ShapeKind.APPLE, miniUnit(room)) }
        }
    }
}

@Composable
private fun AddMini(room: Dp) {
    // The plate row and the bowl row share the room: two rows and the gap
    // between them decide the seat, and the seat decides the piece. Seats a
    // touch larger than the loose shapes, the same part-colour story the game
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
private fun TakeMini(room: Dp) {
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
