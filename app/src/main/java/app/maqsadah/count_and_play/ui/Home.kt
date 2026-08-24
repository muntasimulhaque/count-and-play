package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

/** The home shelf: the question on top, three enormous toy keys below. */
@Composable
fun HomeScreen(copy: Copy, onChoose: (Skill) -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mirrors the gear so the title stays optically centred and can
            // never slide underneath it, whatever the screen width or font.
            Spacer(Modifier.width(48.dp))
            FitTitle(copy.homeTitle(), Modifier.weight(1f))
            GearButton(Modifier, copy.settingsLabel(), onOpenSettings)
        }
        Tile(Skill.COUNT, Blue, BlueEdge, copy.tileCount(), Modifier.weight(1f), onChoose) { CountMini() }
        Tile(Skill.ADD, Green, GreenEdge, copy.tileAdd(), Modifier.weight(1f), onChoose) { AddMini() }
        Tile(Skill.TAKE, Pink, PinkEdge, copy.tileTake(), Modifier.weight(1f), onChoose) { TakeMini() }
    }
}

/**
 * The shelf question on one line, whatever the language or the width: it
 * steps its size down until it fits, so Bengali and English both stay a
 * single calm question instead of an awkward two-line wrap.
 */
@Composable
private fun FitTitle(text: String, modifier: Modifier) {
    var sizeSp by remember(text) { mutableStateOf(SizeTitle.value) }
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

@Composable
private fun Tile(
    skill: Skill,
    rim: Color,
    edge: Color,
    label: String,
    modifier: Modifier,
    onChoose: (Skill) -> Unit,
    mini: @Composable () -> Unit,
) {
    Keycap(
        rim = rim,
        edge = edge,
        fill = rim.copy(alpha = 0.16f).compositeOver(Liner),
        modifier = modifier.fillMaxWidth().padding(vertical = 7.dp),
        onClick = { onChoose(skill) },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { mini() }
        Text(
            label,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            color = Ink,
            fontSize = SizeLabel,
            fontWeight = ToyBold,
            fontFamily = ToyFont,
        )
    }
}

@Composable
private fun GearButton(modifier: Modifier, description: String, onOpenSettings: () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .background(Liner, CircleShape)
            .border(BorderStroke(3.dp, Ink.copy(alpha = 0.14f)), CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onOpenSettings() }
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(26.dp, Ink.copy(alpha = 0.85f))
    }
}

/** A plain gear: a ring with eight stubby teeth. Vector, static, no emoji. */
@Composable
private fun GearIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat()
            val dy = kotlin.math.sin(a).toFloat()
            drawLine(
                color,
                Offset(center.x + dx * r * 0.58f, center.y + dy * r * 0.58f),
                Offset(center.x + dx * r * 0.95f, center.y + dy * r * 0.95f),
                strokeWidth = r * 0.30f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color, radius = r * 0.50f, style = Stroke(width = r * 0.26f))
    }
}

// ---- The three miniatures: each tile shows the game it opens, big enough
// ---- to be read from across a room.

@Composable
private fun MiniShape(kind: ShapeKind, sizeDp: Dp) {
    Canvas(Modifier.size(sizeDp)) { drawCountable(kind, size.minDimension) }
}

@Composable
private fun MiniBox(rim: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(Liner, RoundedCornerShape(18.dp))
            .border(BorderStroke(4.dp, rim), RoundedCornerShape(18.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MiniSeated(kind: ShapeKind, seat: Color) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(46.dp).background(seat, CircleShape))
        MiniShape(kind, 34.dp)
    }
}

@Composable
private fun CountMini() {
    MiniBox(Blue) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { MiniShape(ShapeKind.APPLE, 34.dp) }
        }
    }
}

@Composable
private fun AddMini() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniBox(Blue) { MiniShape(ShapeKind.STAR, 30.dp) }
            MiniBox(Orange) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniShape(ShapeKind.BALL, 30.dp)
                    MiniShape(ShapeKind.BALL, 30.dp)
                }
            }
        }
        MiniBox(Green) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniSeated(ShapeKind.STAR, SeatA)
                MiniSeated(ShapeKind.BALL, SeatB)
                MiniSeated(ShapeKind.BALL, SeatB)
            }
        }
    }
}

@Composable
private fun TakeMini() {
    MiniBox(Pink) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniShape(ShapeKind.APPLE, 34.dp)
            GhostSlot(34.dp)
            GhostSlot(34.dp)
        }
    }
}
