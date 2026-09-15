package app.maqsadah.count_and_play.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
 *
 * This file owns the palette and the drawing pipeline; the silhouettes live in
 * ShapeBodies.kt and the detail work (stems, veins, seeds) in ShapeTrims.kt.
 */
enum class Detail { FULL, PRIMARY, PLAIN }

fun detailFor(cellDp: Float): Detail = when {
    cellDp >= 56f -> Detail.FULL
    cellDp >= 40f -> Detail.PRIMARY
    else -> Detail.PLAIN
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

/** The design box every silhouette is drawn in before scaling to its cell. */
internal const val BOX = 100f

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

/** Draws a path through the builder, the one idiom every silhouette uses. */
internal fun Path.boxed(block: Path.() -> Unit): Path = apply(block)

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
