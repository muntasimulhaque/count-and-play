package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

/**
 * The shelf: what shall we play?
 *
 * Pictures, not words — a 3-year-old cannot read, so a menu of words is not a
 * choice offered to him, it is a choice offered to his father. Each tile draws
 * the opening moment of its own activity in the shape he chose, so the tile
 * *is* a small picture of what will happen.
 *
 * Everything is always here. The app suggests, with one quietly breathing ring,
 * whatever has had the least practice; it never withholds. Choosing badly is not
 * a thing a 3-year-old can do on this screen, which is exactly the point of it.
 */
@Composable
fun Shelf(
    copy: Copy,
    shape: ShapeKind,
    suggested: Skill?,
    dark: Boolean,
    onPick: (Skill) -> Unit,
    onFreePlay: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            copy.ui.shelfTitle,
            color = palette.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 52.dp, bottom = 2.dp),
        )
        val tiles = Skill.entries.map { it as Skill? } + listOf<Skill?>(null)
        tiles.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { skill ->
                    Tile(
                        skill = skill,
                        label = skill?.let(copy::activityName) ?: copy.ui.freePlay,
                        shape = shape,
                        breathing = skill != null && skill == suggested,
                        dark = dark,
                        modifier = Modifier.weight(1f),
                        onClick = { if (skill == null) onFreePlay() else onPick(skill) },
                    )
                }
                // The last row holds one tile; it keeps its half of the width
                // rather than stretching to fill, so every tile is the same size.
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile(
    skill: Skill?,
    label: String,
    shape: ShapeKind,
    breathing: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val accent = accentFor(skill, dark)
    val ground = groundFor(skill, dark)
    val pulse by if (breathing) {
        rememberInfiniteTransition(label = "suggest").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "suggest",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(ground, RoundedCornerShape(26.dp))
            .border(if (breathing) (4 + 4 * pulse).dp else 4.dp, accent, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp)
                .drawBehind { tileArt(skill, shape, palette, accent) },
        )
        Text(
            label,
            color = palette.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

/**
 * The opening moment of each activity, drawn small.
 *
 * Built from the same [drawCountable] the game uses, so a tile can never drift
 * away from what the activity actually looks like.
 */
private fun DrawScope.tileArt(
    skill: Skill?,
    shape: ShapeKind,
    palette: Palette,
    accent: androidx.compose.ui.graphics.Color,
) {
    val colors = colorsFor(shape)
    val w = size.width
    val h = size.height
    val cell = minOf(w / 3.4f, h / 2.1f)

    fun obj(cx: Float, cy: Float, size: Float = cell) {
        translate(cx - size / 2f, cy - size / 2f) {
            drawCountable(shape, size, colors, palette, Detail.PRIMARY)
        }
    }

    fun dish(cx: Float, cy: Float, halfW: Float, halfH: Float) {
        drawRoundRect(
            color = palette.tray,
            topLeft = Offset(cx - halfW, cy - halfH),
            size = Size(halfW * 2, halfH * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfH * 0.45f),
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(cx - halfW, cy - halfH),
            size = Size(halfW * 2, halfH * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfH * 0.45f),
            style = Stroke(width = cell * 0.09f),
        )
    }

    when (skill) {
        // Three in a row, waiting to be tagged.
        Skill.COUNT -> {
            dish(w / 2f, h / 2f, w * 0.46f, cell * 0.62f)
            for (i in 0..2) obj(w / 2f + (i - 1) * cell * 0.86f, h / 2f)
        }
        // A heap above, an empty bowl below: the child moves them down.
        Skill.GIVE_N -> {
            for (i in 0..2) obj(w / 2f + (i - 1) * cell * 0.8f, h * 0.28f, cell * 0.8f)
            dish(w / 2f, h * 0.72f, w * 0.36f, cell * 0.55f)
        }
        // Two dishes, plainly unequal.
        Skill.COMPARE -> {
            dish(w * 0.27f, h / 2f, w * 0.22f, cell * 0.6f)
            dish(w * 0.73f, h / 2f, w * 0.22f, cell * 0.6f)
            for (i in 0..1) obj(w * 0.27f + (i - 0.5f) * cell * 0.7f, h / 2f, cell * 0.62f)
            obj(w * 0.73f, h / 2f, cell * 0.62f)
        }
        // The leaf, with the set plainly still underneath it.
        Skill.HIDDEN -> {
            dish(w / 2f, h / 2f, w * 0.42f, cell * 0.62f)
            obj(w * 0.24f, h / 2f, cell * 0.7f)
            val leaf = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.36f, h * 0.78f)
                quadraticTo(w * 0.44f, h * 0.16f, w * 0.94f, h * 0.24f)
                quadraticTo(w * 0.82f, h * 0.84f, w * 0.36f, h * 0.78f)
                close()
            }
            drawPath(leaf, palette.leaf)
            drawPath(leaf, palette.leafStroke, style = Stroke(cell * 0.08f))
        }
        // Two parts about to become one whole, in their two colours.
        Skill.JOIN -> {
            dish(w * 0.27f, h * 0.32f, w * 0.2f, cell * 0.5f)
            dish(w * 0.73f, h * 0.32f, w * 0.2f, cell * 0.5f)
            obj(w * 0.27f, h * 0.32f, cell * 0.58f)
            obj(w * 0.73f, h * 0.32f, cell * 0.58f)
            dish(w / 2f, h * 0.74f, w * 0.4f, cell * 0.52f)
            drawCircle(palette.partA, cell * 0.3f, Offset(w * 0.38f, h * 0.74f))
            drawCircle(palette.partB, cell * 0.3f, Offset(w * 0.62f, h * 0.74f))
            obj(w * 0.38f, h * 0.74f, cell * 0.56f)
            obj(w * 0.62f, h * 0.74f, cell * 0.56f)
        }
        // A whole, with one plainly leaving it.
        Skill.SEPARATE -> {
            dish(w * 0.36f, h / 2f, w * 0.31f, cell * 0.62f)
            for (i in 0..1) obj(w * 0.36f + (i - 0.5f) * cell * 0.72f, h / 2f, cell * 0.64f)
            dish(w * 0.82f, h / 2f, w * 0.14f, cell * 0.62f)
            obj(w * 0.82f, h / 2f, cell * 0.64f)
        }
        // No rules at all: a loose handful.
        null -> {
            obj(w * 0.30f, h * 0.34f, cell * 0.8f)
            obj(w * 0.68f, h * 0.30f, cell * 0.8f)
            obj(w * 0.36f, h * 0.70f, cell * 0.8f)
            obj(w * 0.72f, h * 0.68f, cell * 0.8f)
        }
    }
}
