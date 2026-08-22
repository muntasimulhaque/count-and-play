package toybox.tools

import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// -- Type ----------------------------------------------------------------------

private val BLACK_PATHS = listOf("C:\\Windows\\Fonts\\ariblk.ttf", "C:\\Windows\\Fonts\\seguibl.ttf")
private val BOLD_PATHS = listOf("C:\\Windows\\Fonts\\arialbd.ttf", "C:\\Windows\\Fonts\\seguibl.ttf")
private val fontCache = HashMap<String, Font>()

/** The device's chunky faces, loaded straight from the system: the same
 *  files the Python generator used, so the lettering stays the lettering. */
fun font(kind: String, size: Int): Font {
    val key = "$kind:$size"
    fontCache[key]?.let { return it }
    val paths = if (kind == "bold") BOLD_PATHS else BLACK_PATHS
    val path = paths.firstOrNull { File(it).exists() } ?: error("no usable font found")
    val f = Font.createFont(Font.TRUETYPE_FONT, File(path)).deriveFont(size.toFloat())
    fontCache[key] = f
    return f
}

private fun frc() = java.awt.font.FontRenderContext(null, true, true)

/** The text's advance width, the measure every layout decision uses. */
fun textAdvance(text: String, f: Font): Double = TextLayout(text, f, frc()).advance.toDouble()

internal fun anchorPos(text: String, f: Font, x: Double, cy: Double, anchor: String): Pair<Double, Double> {
    val l = TextLayout(text, f, frc())
    val baseline = cy + (l.ascent - l.descent) / 2.0
    val drawX = when (anchor) {
        "mm" -> x - l.advance / 2.0
        "lm" -> x
        else -> x
    }
    return drawX to baseline
}

/** The ink bounds in absolute canvas coordinates (for underline bars). */
fun textInkBounds(text: String, f: Font, x: Double, cy: Double, anchor: String): Rectangle2D {
    val l = TextLayout(text, f, frc())
    val (dx, baseline) = anchorPos(text, f, x, cy, anchor)
    val b = l.bounds
    return Rectangle2D.Double(dx + b.minX, baseline + b.minY, b.width, b.height)
}

/**
 * Chunky type with a soft drop and an optional offset tint for depth. The
 * stroke extends [stroke] pixels outside the glyphs, as Pillow's did.
 */
fun stickerText(
    dst: Img,
    x: Double,
    y: Double,
    text: String,
    kind: String,
    size: Int,
    fill: Int,
    stroke: Int = 0,
    strokeFill: Int = WHITE,
    shadow: Quad = Quad(6.0, 14.0, 60, 9.0),
    tint: Int? = null,
    tintOff: Pair<Double, Double> = 9.0 to 11.0,
    anchor: String = "mm",
) {
    val f = font(kind, size)
    val (dx0, base0) = anchorPos(text, f, x, y, anchor)

    val lay = img(dst.width, dst.height)
    val lg = graphicsText(lay)
    drawGlyphs(lg, text, f, dx0 + shadow.a, base0 + shadow.b, INK or (shadow.c shl 24), stroke, INK or (shadow.c shl 24))
    lg.dispose()
    val blurred = gaussianBlur(lay, shadow.d)
    val g = graphicsText(dst)
    g.drawImage(blurred, 0, 0, null)

    if (tint != null) {
        drawGlyphs(g, text, f, dx0 + tintOff.first, base0 + tintOff.second, tint, stroke, strokeFill)
    }
    drawGlyphs(g, text, f, dx0, base0, fill, stroke, strokeFill)
    g.dispose()
}

data class Quad(val a: Double, val b: Double, val c: Int, val d: Double)

internal fun graphicsText(im: Img): Graphics2D = im.createGraphics().apply {
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
}

internal fun drawGlyphs(
    g: Graphics2D,
    text: String,
    f: Font,
    x: Double,
    baseline: Double,
    fill: Int,
    stroke: Int,
    strokeFill: Int,
) {
    val shape = TextLayout(text, f, g.fontRenderContext).getOutline(AffineTransform.getTranslateInstance(x, baseline))
    if (stroke > 0) {
        g.argb(strokeFill)
        g.stroke = BasicStroke(stroke * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(shape)
    }
    g.argb(fill)
    g.fill(shape)
}

// -- Materials -------------------------------------------------------------------

/** Soft radial highlight geometry: centre, radius and falloff as tile fractions. */
private data class Gloss(val cx: Double, val cy: Double, val radius: Double, val peak: Int, val power: Double)

/** A glossy toy ball: shaded sphere, soft sheen, one hot specular, rim light. */
fun ballTile(dia: Int, base: Int): Img {
    val d = max(4, dia)
    val tile = img(d, d)
    val g = graphics(tile)
    g.clip = Ellipse2D.Double(0.0, 0.0, d.toDouble(), d.toDouble())
    g.drawImage(vgrad(d, d, listOf(0.0 to lighten(base, 0.40), 0.55 to base, 1.0 to darken(base, 0.24))), 0, 0, null)
    g.dispose()

    for (gl in listOf(Gloss(0.40, 0.34, 0.46, 140, 1.7), Gloss(0.345, 0.27, 0.17, 235, 2.6))) {
        val layer = img(d, d)
        val px = IntArray(d * d)
        for (y in 0 until d) for (x in 0 until d) {
            val dist = sqrt((x - d * gl.cx).pow2() + (y - d * gl.cy).pow2()) / (d * gl.radius)
            val a = ((1.0 - dist).coerceIn(0.0, 1.0).pow(gl.power) * gl.peak).roundToInt().coerceIn(0, 255)
            px[y * d + x] = a shl 24 or 0xFFFFFF
        }
        layer.raster.setDataElements(0, 0, d, d, px)
        val tg = graphics(tile)
        tg.drawImage(layer, 0, 0, null)
        tg.dispose()
    }

    // Rim light: the bottom crescent of the sphere, quietly lit.
    val body = Area(Ellipse2D.Double(0.0, 0.0, d.toDouble(), d.toDouble()))
    val shifted = Area(Ellipse2D.Double(0.0, -d * 0.05, d.toDouble(), d.toDouble()))
    val crescent = body.apply { subtract(shifted) }
    crescent.intersect(Area(Rectangle2D.Double(0.0, d * 0.74, d.toDouble(), d * 0.26)))
    val coverage = rasterize(crescent, d, d)
    val rim = img(d, d)
    val lit = lighten(base, 0.45)
    val rimPx = IntArray(d * d) { i ->
        val a = (coverage[i] / 255.0 * 0.20 * 255).roundToInt().coerceIn(0, 255)
        (a shl 24) or (lit and 0xFFFFFF)
    }
    rim.raster.setDataElements(0, 0, d, d, rimPx)
    val rg = graphics(tile)
    rg.drawImage(rim, 0, 0, null)
    rg.dispose()
    return tile
}

/** A white plate seen from a child's angle: lit top, shaded well, warm rim. */
fun plateTile(rx: Double, ry: Double): Img {
    val w = (rx * 2).toInt()
    val h = (ry * 2).toInt()
    val tile = img(w, h)
    val ellipse = Ellipse2D.Double(0.0, 0.0, w.toDouble(), h.toDouble())
    val g = graphics(tile)
    g.clip = ellipse
    g.drawImage(vgrad(w, h, listOf(0.0 to WHITE, 0.6 to rgb(252, 250, 245), 1.0 to rgb(233, 225, 207))), 0, 0, null)
    g.dispose()

    // Shaded well along the bottom inner edge.
    val shiftedUp = Area(Ellipse2D.Double(0.0, -ry * 0.16, w.toDouble(), h.toDouble()))
    val innerBottom = Area(ellipse).apply { subtract(shiftedUp) }
    paintAreaBlurred(tile, innerBottom, rgb(198, 188, 166), 0.60, max(2.0, ry * 0.06))

    // Lit lip along the top inner edge.
    val shiftedDown = Area(Ellipse2D.Double(0.0, ry * 0.15, w.toDouble(), h.toDouble()))
    val topLip = Area(ellipse).apply { subtract(shiftedDown) }
    paintAreaBlurred(tile, topLip, WHITE, 0.85, max(1.0, ry * 0.03))
    return tile
}

/** Paint [color] through [area] at [strength], softened by [blur]. */
private fun paintAreaBlurred(dst: Img, area: Area, color: Int, strength: Double, blur: Double) {
    val w = dst.width
    val h = dst.height
    val coverage = rasterize(area, w, h)
    val layer = img(w, h)
    val px = IntArray(w * h) { i ->
        val a = (coverage[i] / 255.0 * strength * 255).roundToInt().coerceIn(0, 255)
        (a shl 24) or (color and 0xFFFFFF)
    }
    layer.raster.setPixels(0, 0, w, h, px)
    val g = graphics(dst)
    g.drawImage(gaussianBlur(layer, blur), 0, 0, null)
    g.dispose()
}

/** Fill an area into a coverage mask (0..255 per pixel), hard edges as PIL drew them. */
private fun rasterize(area: Area, w: Int, h: Int): ByteArray {
    val mask = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
    val g = mask.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
    g.fill(area)
    g.dispose()
    val buf = ByteArray(w * h)
    mask.raster.getDataElements(0, 0, w, h, buf)
    return buf
}
