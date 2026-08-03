package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

/** The home shelf: the question on top, three enormous doors below. */
@Composable
fun HomeScreen(copy: Copy, onChoose: (Skill) -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text(
                copy.homeTitle(),
                Modifier.align(Alignment.Center).padding(vertical = 10.dp),
                color = Ink,
                fontSize = SizeTitle,
                fontWeight = ToyBlack,
                fontFamily = ToyFont,
            )
            GearButton(Modifier.align(Alignment.CenterEnd), onOpenSettings)
        }
        Tile(Skill.COUNT, Blue, copy.tileCount(), Modifier.weight(1f), onChoose) { CountMini() }
        Tile(Skill.ADD, Green, copy.tileAdd(), Modifier.weight(1f), onChoose) { AddMini() }
        Tile(Skill.TAKE, Pink, copy.tileTake(), Modifier.weight(1f), onChoose) { TakeMini() }
    }
}

@Composable
private fun Tile(
    skill: Skill,
    rim: Color,
    label: String,
    modifier: Modifier,
    onChoose: (Skill) -> Unit,
    mini: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .background(rim.copy(alpha = 0.14f), RoundedCornerShape(Corner))
            .border(BorderStroke(OutlineWidth, rim), RoundedCornerShape(Corner))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onChoose(skill) },
        contentAlignment = Alignment.Center,
    ) {
        mini()
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
private fun GearButton(modifier: Modifier, onOpenSettings: () -> Unit) {
    Box(
        modifier.size(52.dp).clickable(remember { MutableInteractionSource() }, indication = null) { onOpenSettings() },
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(30.dp, Ink)
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

// ---- The three miniatures: each tile shows the game it opens ----

@Composable
private fun MiniShape(kind: ShapeKind, sizeDp: Dp) {
    Canvas(Modifier.size(sizeDp)) { drawCountable(kind, size.minDimension) }
}

@Composable
private fun MiniBox(rim: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(Liner, RoundedCornerShape(14.dp))
            .border(BorderStroke(3.dp, rim), RoundedCornerShape(14.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MiniSeated(kind: ShapeKind, seat: Color) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(34.dp).background(seat, CircleShape))
        MiniShape(kind, 26.dp)
    }
}

@Composable
private fun CountMini() {
    MiniBox(Blue) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { MiniShape(ShapeKind.APPLE, 24.dp) }
        }
    }
}

@Composable
private fun AddMini() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBox(Blue) { MiniShape(ShapeKind.STAR, 22.dp) }
            MiniBox(Orange) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniShape(ShapeKind.BALL, 22.dp)
                    MiniShape(ShapeKind.BALL, 22.dp)
                }
            }
        }
        MiniBox(Green) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniShape(ShapeKind.APPLE, 24.dp)
            GhostSlot(24.dp)
            GhostSlot(24.dp)
        }
    }
}
