package toybox.tools

import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.File

// -- Type ----------------------------------------------------------------------

/** The repo root the generators run from; [MakeArtMain] sets it, so the store
 *  art can set the app's own bundled face instead of an OS default. */
var artRoot: File = File(".")

private fun fontPaths(kind: String): List<File> {
    val bundled = if (kind == "bold") "baloo2_bold.ttf" else "baloo2_extrabold.ttf"
    return listOf(artRoot.resolve("app/src/main/res/font/$bundled"))
}

private val fontCache = HashMap<String, Font>()

/** Baloo 2, the same bundled face the app renders, so shelf and store match. */
fun font(kind: String, size: Int): Font {
    val key = "$kind:$size"
    fontCache[key]?.let { return it }
    val path = fontPaths(kind).firstOrNull { it.isFile } ?: error("bundled font missing: run from the repo root")
    val f = Font.createFont(Font.TRUETYPE_FONT, path).deriveFont(size.toFloat())
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
