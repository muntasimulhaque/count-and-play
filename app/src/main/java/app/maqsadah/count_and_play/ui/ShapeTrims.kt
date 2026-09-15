package app.maqsadah.count_and_play.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.maqsadah.count_and_play.core.ShapeKind

// ---- Trims: stems, veins, seeds and grooves, dropped first as cells shrink --

private val StemBrown = Color(0xFF7A5233)
private val LeafDark = Color(0xFF1B6B37)
private val MelonRind = Color(0xFF3F8A55)
private val MelonSeed = Color(0xFF3A1018)

/** The detail each shape wears at [detail]'s level, over its filled body. */
internal fun DrawScope.drawTrim(kind: ShapeKind, s: Float, colors: ShapeColors, detail: Detail) {
    when (kind) {
        ShapeKind.APPLE -> trimApple(s, detail)
        ShapeKind.PEAR -> trimPear(s)
        ShapeKind.LEAF -> trimLeaf(s, colors, detail)
        ShapeKind.BLOCK -> trimBlock(s, colors)
        ShapeKind.MELON -> trimMelon(s, detail)
        ShapeKind.CARROT -> trimCarrot(s, colors, detail)
        ShapeKind.TULIP -> trimTulip(s, detail)
        ShapeKind.BALL -> trimBall(s)
        ShapeKind.STAR -> Unit
        ShapeKind.BEAD -> drawCircle(
            colors.stroke, radius = 17f * s,
            center = Offset(50f * s, 50f * s), style = Stroke(3f * s),
        )
    }
}

private fun DrawScope.trimApple(s: Float, detail: Detail) {
    drawLine(StemBrown, Offset(50f * s, 26f * s), Offset(56f * s, 6f * s), 5f * s, StrokeCap.Round)
    if (detail == Detail.FULL) {
        val leaf = Path().boxed {
            moveTo(54f * s, 14f * s)
            quadraticTo(74f * s, 2f * s, 84f * s, 14f * s)
            quadraticTo(70f * s, 22f * s, 54f * s, 14f * s)
            close()
        }
        drawPath(leaf, Green)
        drawPath(leaf, LeafDark, style = Stroke(2.6f * s))
    }
}

private fun DrawScope.trimPear(s: Float) {
    drawLine(StemBrown, Offset(50f * s, 14f * s), Offset(50f * s, 2f * s), 4.6f * s, StrokeCap.Round)
}

private fun DrawScope.trimLeaf(s: Float, colors: ShapeColors, detail: Detail) {
    drawLine(colors.stroke, Offset(18f * s, 80f * s), Offset(80f * s, 22f * s), 3.2f * s, StrokeCap.Round)
    if (detail == Detail.FULL) {
        for (i in 1..3) {
            val t = i / 4f
            val x = 18f + (80f - 18f) * t
            val y = 80f + (22f - 80f) * t
            drawLine(colors.stroke, Offset(x * s, y * s), Offset((x + 6f) * s, (y - 14f) * s), 2.2f * s, StrokeCap.Round)
            drawLine(colors.stroke, Offset(x * s, y * s), Offset((x - 14f) * s, (y + 6f) * s), 2.2f * s, StrokeCap.Round)
        }
    }
}

private fun DrawScope.trimBlock(s: Float, colors: ShapeColors) {
    drawLine(colors.stroke, Offset(16f * s, 36f * s), Offset(58f * s, 36f * s), 3f * s)
    drawLine(colors.stroke, Offset(58f * s, 36f * s), Offset(58f * s, 88f * s), 3f * s)
    drawLine(colors.stroke, Offset(58f * s, 36f * s), Offset(88f * s, 12f * s), 3f * s)
}

private fun DrawScope.trimMelon(s: Float, detail: Detail) {
    drawLine(MelonRind, Offset(8f * s, 34f * s), Offset(92f * s, 34f * s), 9f * s)
    if (detail == Detail.FULL) {
        // Four seeds in an arc following the rind, deliberately NOT
        // two-above-one, which the eye reads as two eyes and a mouth.
        // Pareidolia is still a face, and this app does not draw faces.
        for ((x, y) in listOf(28f to 52f, 42f to 60f, 58f to 60f, 72f to 52f)) {
            drawOval(MelonSeed, Offset((x - 3.5f) * s, (y - 5f) * s), Size(7f * s, 10f * s))
        }
    }
}

private fun DrawScope.trimCarrot(s: Float, colors: ShapeColors, detail: Detail) {
    for (fx in listOf(-26f, 0f, 26f)) {
        drawLine(Green, Offset(50f * s, 30f * s), Offset((50f + fx) * s, 4f * s), 5.4f * s, StrokeCap.Round)
    }
    if (detail == Detail.FULL) {
        for (y in listOf(44f, 58f, 72f)) {
            val half = (24f - (y - 44f) * 0.42f) * 0.6f
            drawLine(colors.stroke, Offset((50f - half) * s, y * s), Offset((50f + half) * s, y * s), 2.6f * s, StrokeCap.Round)
        }
    }
}

private fun DrawScope.trimTulip(s: Float, detail: Detail) {
    drawLine(Green, Offset(50f * s, 78f * s), Offset(50f * s, 98f * s), 5f * s, StrokeCap.Round)
    if (detail == Detail.FULL) {
        drawLine(Green, Offset(50f * s, 88f * s), Offset(26f * s, 80f * s), 5f * s, StrokeCap.Round)
        drawLine(Green, Offset(50f * s, 92f * s), Offset(74f * s, 86f * s), 5f * s, StrokeCap.Round)
    }
}

private fun DrawScope.trimBall(s: Float) {
    val band = Stroke(width = 9f * s, cap = StrokeCap.Butt)
    val arc = Path().boxed { moveTo(9f * s, 40f * s); quadraticTo(50f * s, 30f * s, 91f * s, 40f * s) }
    val arc2 = Path().boxed { moveTo(12f * s, 66f * s); quadraticTo(50f * s, 76f * s, 88f * s, 66f * s) }
    drawPath(arc, Liner, style = band)
    drawPath(arc2, Liner, style = band)
}
