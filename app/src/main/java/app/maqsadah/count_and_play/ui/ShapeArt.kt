package app.maqsadah.count_and_play.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import app.maqsadah.count_and_play.core.ShapeKind

/**
 * The ten countable objects, drawn as vector paths.
 *
 * Never emoji: emoji depend on a font we do not control, render differently on
 * every device, and cannot be animated beyond scaling a glyph. Drawing them
 * ourselves also means the no-animate-beings rule is guaranteed by
 * construction rather than by review — there is no code path here that can
 * produce a face.
 *
 * Everything is designed in a 100x100 box and scaled to the cell, so a shape is
 * identical at every size and on every screen.
 */
enum class Detail { FULL, PRIMARY, PLAIN, MINIMAL }

fun detailFor(cellDp: Float): Detail = when {
    cellDp >= 56f -> Detail.FULL
    cellDp >= 40f -> Detail.PRIMARY
    cellDp >= 32f -> Detail.PLAIN
    else -> Detail.MINIMAL
}

private const val BOX = 100f

/** Deterministic per-slot tilt: lively, but never re-randomised on recomposition. */
fun tiltFor(slot: Int): Float = ((slot * 37) % 9 - 4).toFloat()

fun DrawScope.drawCountable(
    kind: ShapeKind,
    cell: Float,
    colors: ShapeColors,
    palette: Palette,
    detail: Detail,
) {
    val s = cell / BOX
    val outline = Stroke(
        width = 4f * s,
        join = StrokeJoin.Round,
        cap = StrokeCap.Round,
    )

    if (detail != Detail.MINIMAL) {
        drawOval(
            color = palette.ink.copy(alpha = if (palette.dark) 0.34f else 0.14f),
            topLeft = Offset(0.14f * cell + 0.05f * cell, 0.86f * cell),
            size = Size(0.72f * cell, 0.16f * cell),
        )
    }

    val body = bodyPath(kind, s)
    drawPath(body, colors.fill, style = Fill)

    if (detail == Detail.FULL || detail == Detail.PRIMARY) {
        // One hard-edged facet catching the upper-left light. No blur, no
        // gradient — a wooden bevel, not a highlight.
        clipPath(body) {
            drawPath(facetPath(kind, s), colors.facet, style = Fill)
        }
    }

    drawPath(body, colors.stroke, style = outline)
    if (detail == Detail.FULL || detail == Detail.PRIMARY) {
        drawTrim(kind, s, colors, palette, detail)
    }
}

/** The silhouette. Ten shapes, ten distinct silhouette classes, no collisions. */
private fun bodyPath(kind: ShapeKind, s: Float): Path {
    val p = Path()
    fun m(x: Float, y: Float) = p.moveTo(x * s, y * s)
    fun l(x: Float, y: Float) = p.lineTo(x * s, y * s)
    fun q(cx: Float, cy: Float, x: Float, y: Float) =
        p.quadraticTo(cx * s, cy * s, x * s, y * s)

    when (kind) {
        // A circle with a dimple and a stalk.
        ShapeKind.APPLE -> {
            m(50f, 24f)
            q(58f, 14f, 70f, 20f)
            q(92f, 32f, 88f, 58f)
            q(84f, 88f, 50f, 90f)
            q(16f, 88f, 12f, 58f)
            q(8f, 32f, 30f, 20f)
            q(42f, 14f, 50f, 24f)
            p.close()
        }
        // The only shape whose width changes down its length.
        ShapeKind.PEAR -> {
            m(50f, 12f)
            q(70f, 16f, 66f, 40f)
            q(62f, 56f, 78f, 66f)
            q(92f, 78f, 78f, 88f)
            q(64f, 96f, 50f, 94f)
            q(36f, 96f, 22f, 88f)
            q(8f, 78f, 22f, 66f)
            q(38f, 56f, 34f, 40f)
            q(30f, 16f, 50f, 12f)
            p.close()
        }
        // Fat-armed, not spiky: spiky stars read badly at small sizes.
        ShapeKind.STAR -> {
            val cx = 50f
            val cy = 52f
            val outer = 44f
            val inner = 25f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outer else inner
                val a = Math.toRadians((-90 + i * 36).toDouble())
                val x = cx + r * kotlin.math.cos(a).toFloat()
                val y = cy + r * kotlin.math.sin(a).toFloat()
                if (i == 0) m(x, y) else l(x, y)
            }
            p.close()
        }
        // A tilted lens with one sharp end.
        ShapeKind.LEAF -> {
            m(14f, 84f)
            q(24f, 30f, 86f, 16f)
            q(72f, 74f, 14f, 84f)
            p.close()
        }
        // The only orthogonal shape — instantly separable from every fruit.
        ShapeKind.BLOCK -> {
            m(16f, 36f)
            l(46f, 12f)
            l(88f, 12f)
            l(88f, 62f)
            l(58f, 88f)
            l(16f, 88f)
            p.close()
        }
        // The only shape with a hole in it.
        ShapeKind.BEAD -> {
            p.addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s))
            val hole = Path().apply { addOval(Rect(33f * s, 33f * s, 67f * s, 67f * s)) }
            return Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, p, hole)
        }
        // A half-disc, flat side up.
        ShapeKind.MELON -> {
            m(8f, 30f)
            l(92f, 30f)
            q(92f, 92f, 50f, 92f)
            q(8f, 92f, 8f, 30f)
            p.close()
        }
        // The only downward-pointing wedge.
        ShapeKind.CARROT -> {
            m(26f, 28f)
            l(74f, 28f)
            q(70f, 62f, 54f, 92f)
            q(50f, 97f, 46f, 92f)
            q(30f, 62f, 26f, 28f)
            p.close()
        }
        // A three-pointed cup on a stalk. Distinct from the carrot by pointing up.
        ShapeKind.TULIP -> {
            m(24f, 44f)
            l(35f, 24f)
            l(42f, 44f)
            l(50f, 22f)
            l(58f, 44f)
            l(65f, 24f)
            l(76f, 44f)
            q(76f, 78f, 50f, 80f)
            q(24f, 78f, 24f, 44f)
            p.close()
        }
        // The only clean circle: nothing else is un-notched, un-holed, un-tipped.
        ShapeKind.BALL -> p.addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s))
    }
    return p
}

/** The lit facet, upper-left, clipped to the body. */
private fun facetPath(kind: ShapeKind, s: Float): Path {
    val p = Path()
    when (kind) {
        ShapeKind.BLOCK -> {
            // The block states the light model most plainly: a lit top face.
            p.moveTo(16f * s, 36f * s)
            p.lineTo(46f * s, 12f * s)
            p.lineTo(88f * s, 12f * s)
            p.lineTo(58f * s, 36f * s)
            p.close()
        }
        else -> {
            p.moveTo(6f * s, 62f * s)
            p.quadraticTo(10f * s, 16f * s, 58f * s, 8f * s)
            p.quadraticTo(30f * s, 26f * s, 30f * s, 62f * s)
            p.close()
        }
    }
    return p
}

/** Stems, veins, seeds and grooves — dropped first as the cell gets smaller. */
private fun DrawScope.drawTrim(
    kind: ShapeKind,
    s: Float,
    colors: ShapeColors,
    palette: Palette,
    detail: Detail,
) {
    val stemColor = Color(0xFF7A5233)
    val thin = Stroke(width = 3.4f * s, cap = StrokeCap.Round)

    when (kind) {
        ShapeKind.APPLE -> {
            drawLine(stemColor, Offset(50f * s, 26f * s), Offset(56f * s, 6f * s), 5f * s, StrokeCap.Round)
            if (detail == Detail.FULL) {
                val leaf = Path().apply {
                    moveTo(54f * s, 14f * s)
                    quadraticTo(74f * s, 2f * s, 84f * s, 14f * s)
                    quadraticTo(70f * s, 22f * s, 54f * s, 14f * s)
                    close()
                }
                drawPath(leaf, palette.leaf)
                drawPath(leaf, palette.leafStroke, style = Stroke(2.6f * s))
            }
        }
        ShapeKind.PEAR ->
            drawLine(stemColor, Offset(50f * s, 14f * s), Offset(50f * s, 2f * s), 4.6f * s, StrokeCap.Round)

        ShapeKind.LEAF -> {
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
        ShapeKind.BLOCK -> {
            drawLine(colors.stroke, Offset(16f * s, 36f * s), Offset(58f * s, 36f * s), 3f * s)
            drawLine(colors.stroke, Offset(58f * s, 36f * s), Offset(58f * s, 88f * s), 3f * s)
            drawLine(colors.stroke, Offset(58f * s, 36f * s), Offset(88f * s, 12f * s), 3f * s)
        }
        ShapeKind.MELON -> {
            drawLine(Color(0xFF3F8A55), Offset(8f * s, 34f * s), Offset(92f * s, 34f * s), 9f * s)
            if (detail == Detail.FULL) {
                // Four seeds in an arc following the rind — deliberately NOT
                // two-above-one, which the eye reads as two eyes and a mouth.
                // Pareidolia is still a face, and this app does not draw faces.
                for ((x, y) in listOf(28f to 52f, 42f to 60f, 58f to 60f, 72f to 52f)) {
                    drawOval(
                        Color(0xFF3A1018),
                        topLeft = Offset((x - 3.5f) * s, (y - 5f) * s),
                        size = Size(7f * s, 10f * s),
                    )
                }
            }
        }
        ShapeKind.CARROT -> {
            for (fx in listOf(-26f, 0f, 26f)) {
                drawLine(
                    palette.leaf,
                    Offset(50f * s, 30f * s),
                    Offset((50f + fx) * s, 4f * s),
                    5.4f * s,
                    StrokeCap.Round,
                )
            }
            if (detail == Detail.FULL) {
                for (y in listOf(44f, 58f, 72f)) {
                    val half = (24f - (y - 44f) * 0.42f) * 0.6f
                    drawLine(colors.stroke, Offset((50f - half) * s, y * s), Offset((50f + half) * s, y * s), 2.6f * s, StrokeCap.Round)
                }
            }
        }
        ShapeKind.TULIP -> {
            drawLine(palette.leaf, Offset(50f * s, 78f * s), Offset(50f * s, 98f * s), 5f * s, StrokeCap.Round)
            if (detail == Detail.FULL) {
                drawLine(palette.leaf, Offset(50f * s, 88f * s), Offset(26f * s, 80f * s), 5f * s, StrokeCap.Round)
                drawLine(palette.leaf, Offset(50f * s, 92f * s), Offset(74f * s, 86f * s), 5f * s, StrokeCap.Round)
            }
        }
        ShapeKind.BALL -> {
            val band = Stroke(width = 9f * s, cap = StrokeCap.Butt)
            val arc = Path().apply {
                moveTo(9f * s, 40f * s)
                quadraticTo(50f * s, 30f * s, 91f * s, 40f * s)
            }
            val arc2 = Path().apply {
                moveTo(12f * s, 66f * s)
                quadraticTo(50f * s, 76f * s, 88f * s, 66f * s)
            }
            drawPath(arc, palette.trayLiner, style = band)
            drawPath(arc2, palette.trayLiner, style = band)
        }
        ShapeKind.STAR, ShapeKind.BEAD -> Unit
    }
    if (kind == ShapeKind.BEAD) {
        drawCircle(
            colors.stroke,
            radius = 17f * s,
            center = Offset(50f * s, 50f * s),
            style = Stroke(3f * s),
        )
    }
}

/**
 * An emptied slot.
 *
 * Not a faded object: a 25%-alpha apple is invisible in daylight and, worse,
 * asks a 3-year-old to understand "this is here but counts as not here" — at
 * the age where inhibition is at its floor. An outline is not an object, so it
 * cannot be miscounted.
 */
fun DrawScope.drawEmptySlot(cell: Float, palette: Palette) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(0.11f * cell, 0.09f * cell), 0f)
    drawCircle(
        color = palette.slotOutline,
        radius = 0.34f * cell,
        center = Offset(cell / 2f, cell / 2f),
        style = Stroke(width = 0.035f * cell, pathEffect = dash),
    )
}
