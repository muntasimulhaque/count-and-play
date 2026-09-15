package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.Skill

/** The home shelf: the question on top, three enormous toy keys below. */
@Composable
fun HomeScreen(copy: Copy, onChoose: (Skill) -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mirrors the gear so the title stays optically centered and can
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

/** One toy key: a white cap on the neutral sand edge, its scene centered inside. */
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
        // The hub: one thick ring, so the center stays a hole rather than a
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
