package app.maqsadah.count_and_play.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.maqsadah.count_and_play.core.ShapeKind

/**
 * The ten countable objects, drawn as vector paths. Never emoji: emoji depend
 * on a font we do not control, and drawing the shapes ourselves means the
 * no-animate-beings rule is guaranteed by construction: no code path here can
 * produce a face. Everything is designed in a 100x100 box and scaled to the
 * cell, so a shape is identical at every size and on every screen.
 */
enum class Detail { FULL, PRIMARY, PLAIN, MINIMAL }

fun detailFor(cellDp: Float): Detail = when {
    cellDp >= 56f -> Detail.FULL
    cellDp >= 40f -> Detail.PRIMARY
    cellDp >= 32f -> Detail.PLAIN
    else -> Detail.MINIMAL
}

/** Fill, outline and top facet for each object. */
data class ShapeColors(val fill: Color, val stroke: Color, val facet: Color)

/** Each saturated fill keeps a much darker outline of its own hue, so it
 * cannot dissolve into the white liner of a tray. */
private val shapeColors = mapOf(
    ShapeKind.APPLE to ShapeColors(Color(0xFFE33B2C), Color(0xFF8C1D12), Color(0xFFF4695C)),
    ShapeKind.PEAR to ShapeColors(Color(0xFF8FB023), Color(0xFF4C6110), Color(0xFFB2CE46)),
    ShapeKind.STAR to ShapeColors(Color(0xFFF5AC17), Color(0xFF8A5A05), Color(0xFFFFC94F)),
    ShapeKind.LEAF to ShapeColors(Color(0xFF2F9E60), Color(0xFF135233), Color(0xFF56BC85)),
    ShapeKind.BLOCK to ShapeColors(Color(0xFF2D6FDB), Color(0xFF123C7A), Color(0xFF5A95EE)),
    ShapeKind.BEAD to ShapeColors(Color(0xFF9046B5), Color(0xFF4C215F), Color(0xFFB073CE)),
    ShapeKind.MELON to ShapeColors(Color(0xFFEE3D5F), Color(0xFF8C1B31), Color(0xFFFA7391)),
    ShapeKind.CARROT to ShapeColors(Color(0xFFF06A0E), Color(0xFF8A3A05), Color(0xFFFF9440)),
    ShapeKind.TULIP to ShapeColors(Color(0xFFE04384), Color(0xFF821E45), Color(0xFFF278AA)),
    ShapeKind.BALL to ShapeColors(Color(0xFF0EA0AE), Color(0xFF04525A), Color(0xFF44C0CC)),
)

fun colorsFor(shape: ShapeKind): ShapeColors =
    shapeColors[shape] ?: shapeColors.getValue(ShapeKind.BALL)

private const val BOX = 100f
private val StemBrown = Color(0xFF7A5233)
private val LeafDark = Color(0xFF1B6B37)
private val MelonRind = Color(0xFF3F8A55)
private val MelonSeed = Color(0xFF3A1018)

/** The mark of an emptied slot; deliberately quiet so it is not read as an object. */
private val GhostStroke = Color(0xFFB7C0CC)

/** [cell] is in pixels; detail defaults to what that size can still show. */
fun DrawScope.drawCountable(kind: ShapeKind, cell: Float, detail: Detail = detailFor(cell / density)) {
    val s = cell / BOX
    val colors = colorsFor(kind)
    val body = bodyPath(kind, s)
    drawPath(body, colors.fill, style = Fill)
    if (detail == Detail.FULL || detail == Detail.PRIMARY) {
        // One hard-edged facet catching the upper-left light. No blur, no
        // gradient: a bevel, not a highlight, so the look stays flat.
        clipPath(body) { drawPath(facetPath(kind, s), colors.facet, style = Fill) }
    }
    drawPath(body, colors.stroke, style = Stroke(width = 4f * s, join = StrokeJoin.Round, cap = StrokeCap.Round))
    if (detail == Detail.FULL || detail == Detail.PRIMARY) drawTrim(kind, s, colors, detail)
}

// ---- Bodies: ten silhouettes, ten distinct classes, no collisions ----------

private fun bodyPath(kind: ShapeKind, s: Float): Path = when (kind) {
    ShapeKind.APPLE -> appleBody(s)
    ShapeKind.PEAR -> pearBody(s)
    ShapeKind.STAR -> starBody(s)
    ShapeKind.LEAF -> leafBody(s)
    ShapeKind.BLOCK -> blockBody(s)
    ShapeKind.BEAD -> beadBody(s)
    ShapeKind.MELON -> melonBody(s)
    ShapeKind.CARROT -> carrotBody(s)
    ShapeKind.TULIP -> tulipBody(s)
    ShapeKind.BALL -> ballBody(s)
}

private fun Path.boxed(block: Path.() -> Unit): Path = apply(block)

private fun appleBody(s: Float): Path = Path().boxed {
    // A circle with a dimple and a stalk.
    moveTo(50f * s, 24f * s)
    quadraticTo(58f * s, 14f * s, 70f * s, 20f * s)
    quadraticTo(92f * s, 32f * s, 88f * s, 58f * s)
    quadraticTo(84f * s, 88f * s, 50f * s, 90f * s)
    quadraticTo(16f * s, 88f * s, 12f * s, 58f * s)
    quadraticTo(8f * s, 32f * s, 30f * s, 20f * s)
    quadraticTo(42f * s, 14f * s, 50f * s, 24f * s)
    close()
}

private fun pearBody(s: Float): Path = Path().boxed {
    // The only shape whose width changes down its length.
    moveTo(50f * s, 12f * s)
    quadraticTo(70f * s, 16f * s, 66f * s, 40f * s)
    quadraticTo(62f * s, 56f * s, 78f * s, 66f * s)
    quadraticTo(92f * s, 78f * s, 78f * s, 88f * s)
    quadraticTo(64f * s, 96f * s, 50f * s, 94f * s)
    quadraticTo(36f * s, 96f * s, 22f * s, 88f * s)
    quadraticTo(8f * s, 78f * s, 22f * s, 66f * s)
    quadraticTo(38f * s, 56f * s, 34f * s, 40f * s)
    quadraticTo(30f * s, 16f * s, 50f * s, 12f * s)
    close()
}

private fun starBody(s: Float): Path = Path().boxed {
    // Fat-armed, not spiky: spiky stars read badly at small sizes.
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 44f else 25f
        val a = Math.toRadians((-90 + i * 36).toDouble())
        val x = (50f + r * kotlin.math.cos(a).toFloat()) * s
        val y = (52f + r * kotlin.math.sin(a).toFloat()) * s
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun leafBody(s: Float): Path = Path().boxed {
    // A tilted lens with one sharp end.
    moveTo(14f * s, 84f * s)
    quadraticTo(24f * s, 30f * s, 86f * s, 16f * s)
    quadraticTo(72f * s, 74f * s, 14f * s, 84f * s)
    close()
}

private fun blockBody(s: Float): Path = Path().boxed {
    // The only orthogonal shape, instantly separable from every fruit.
    moveTo(16f * s, 36f * s)
    lineTo(46f * s, 12f * s)
    lineTo(88f * s, 12f * s)
    lineTo(88f * s, 62f * s)
    lineTo(58f * s, 88f * s)
    lineTo(16f * s, 88f * s)
    close()
}

private fun beadBody(s: Float): Path {
    // The only shape with a hole in it.
    val outer = Path().apply { addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s)) }
    val hole = Path().apply { addOval(Rect(33f * s, 33f * s, 67f * s, 67f * s)) }
    return Path.combine(PathOperation.Difference, outer, hole)
}

private fun melonBody(s: Float): Path = Path().boxed {
    // A half-disc, flat side up.
    moveTo(8f * s, 30f * s)
    lineTo(92f * s, 30f * s)
    quadraticTo(92f * s, 92f * s, 50f * s, 92f * s)
    quadraticTo(8f * s, 92f * s, 8f * s, 30f * s)
    close()
}

private fun carrotBody(s: Float): Path = Path().boxed {
    // The only downward-pointing wedge.
    moveTo(26f * s, 28f * s)
    lineTo(74f * s, 28f * s)
    quadraticTo(70f * s, 62f * s, 54f * s, 92f * s)
    quadraticTo(50f * s, 97f * s, 46f * s, 92f * s)
    quadraticTo(30f * s, 62f * s, 26f * s, 28f * s)
    close()
}

private fun tulipBody(s: Float): Path = Path().boxed {
    // A three-pointed cup on a stalk. Distinct from the carrot by pointing up.
    moveTo(24f * s, 44f * s)
    lineTo(35f * s, 24f * s)
    lineTo(42f * s, 44f * s)
    lineTo(50f * s, 22f * s)
    lineTo(58f * s, 44f * s)
    lineTo(65f * s, 24f * s)
    lineTo(76f * s, 44f * s)
    quadraticTo(76f * s, 78f * s, 50f * s, 80f * s)
    quadraticTo(24f * s, 78f * s, 24f * s, 44f * s)
    close()
}

private fun ballBody(s: Float): Path = Path().boxed {
    // The only clean circle: nothing else is un-notched, un-holed, un-tipped.
    addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s))
}

/** The lit facet, upper-left, clipped to the body. */
private fun facetPath(kind: ShapeKind, s: Float): Path = when (kind) {
    ShapeKind.BLOCK ->
        // The block states the light model most plainly: a lit top face.
        Path().boxed {
            moveTo(16f * s, 36f * s)
            lineTo(46f * s, 12f * s)
            lineTo(88f * s, 12f * s)
            lineTo(58f * s, 36f * s)
            close()
        }
    else ->
        Path().boxed {
            moveTo(6f * s, 62f * s)
            quadraticTo(10f * s, 16f * s, 58f * s, 8f * s)
            quadraticTo(30f * s, 26f * s, 30f * s, 62f * s)
            close()
        }
}

// ---- Trims: stems, veins, seeds and grooves, dropped first as cells shrink --

private fun DrawScope.drawTrim(kind: ShapeKind, s: Float, colors: ShapeColors, detail: Detail) {
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

/**
 * An emptied slot. Not a faded object: a 25%-alpha apple is invisible in
 * daylight and, worse, asks a 3-year-old to understand "this is here but
 * counts as not here". An outline is not an object, so it cannot be miscounted.
 */
fun DrawScope.drawEmptySlot(cell: Float) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(0.11f * cell, 0.09f * cell), 0f)
    drawCircle(GhostStroke, radius = 0.34f * cell, center = center,
        style = Stroke(width = 0.035f * cell, pathEffect = dash))
}
