package toybox.tools

import java.awt.BasicStroke
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D

/*
 * The store art draws the app's own objects: the exact bodies, facets and
 * outlines the Compose UI draws in ShapeArt.kt, ported to Java2D in the same
 * 100x100 design box. The shelf and the store therefore speak one language:
 * saturated fill, one hard facet, a fat outline of the same hue, and the
 * navy counting chip that is the app's signature mark.
 */

internal data class FlatColors(val fill: Int, val stroke: Int, val facet: Int)

internal val APPLE_C = FlatColors(rgb(227, 59, 44), rgb(140, 29, 18), rgb(244, 105, 92))
internal val STAR_C = FlatColors(rgb(245, 172, 23), rgb(138, 90, 5), rgb(255, 201, 79))
internal val BALL_C = FlatColors(rgb(14, 160, 174), rgb(4, 82, 90), rgb(68, 192, 204))
internal val CARROT_C = FlatColors(rgb(240, 106, 14), rgb(138, 58, 5), rgb(255, 148, 64))

internal val CHIP_BLUE = rgb(39, 53, 122)
internal val LINER = rgb(255, 255, 255)
internal val RIM_BLUE = rgb(28, 169, 232)
internal val RIM_GREEN = rgb(51, 168, 82)
internal val SEAT_A = rgb(207, 233, 251)
internal val SEAT_B = rgb(253, 227, 196)
internal val STEM_BROWN = rgb(122, 82, 51)
internal val LEAF_DARK = rgb(27, 107, 55)

private fun Path2D.Double.q(cx: Double, cy: Double, x: Double, y: Double) =
    apply { quadTo(cx, cy, x, y) }

/** The apple body from ShapeArt.kt, translated to (x, y) at size [s]. */
private fun applePath(x: Double, y: Double, s: Double): Path2D.Double = Path2D.Double().apply {
    moveTo(x + 50 * s, y + 24 * s)
    q(x + 58 * s, y + 14 * s, x + 70 * s, y + 20 * s)
    q(x + 92 * s, y + 32 * s, x + 88 * s, y + 58 * s)
    q(x + 84 * s, y + 88 * s, x + 50 * s, y + 90 * s)
    q(x + 16 * s, y + 88 * s, x + 12 * s, y + 58 * s)
    q(x + 8 * s, y + 32 * s, x + 30 * s, y + 20 * s)
    q(x + 42 * s, y + 14 * s, x + 50 * s, y + 24 * s)
    closePath()
}

private fun facetPath(x: Double, y: Double, s: Double): Path2D.Double = Path2D.Double().apply {
    moveTo(x + 6 * s, y + 62 * s)
    q(x + 10 * s, y + 16 * s, x + 58 * s, y + 8 * s)
    q(x + 30 * s, y + 26 * s, x + 30 * s, y + 62 * s)
    closePath()
}

private fun starPath(x: Double, y: Double, s: Double): Path2D.Double = Path2D.Double().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 44.0 else 25.0
        val a = Math.toRadians(-90.0 + i * 36)
        val px = x + (50 + r * Math.cos(a)) * s
        val py = y + (52 + r * Math.sin(a)) * s
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    closePath()
}

private fun leafPath(x: Double, y: Double, s: Double): Path2D.Double = Path2D.Double().apply {
    moveTo(x + 54 * s, y + 14 * s)
    q(x + 74 * s, y + 2 * s, x + 84 * s, y + 14 * s)
    q(x + 70 * s, y + 22 * s, x + 54 * s, y + 14 * s)
    closePath()
}

private fun carrotPath(x: Double, y: Double, s: Double): Path2D.Double = Path2D.Double().apply {
    moveTo(x + 26 * s, y + 28 * s)
    lineTo(x + 74 * s, y + 28 * s)
    q(x + 70 * s, y + 62 * s, x + 54 * s, y + 92 * s)
    q(x + 50 * s, y + 97 * s, x + 46 * s, y + 92 * s)
    q(x + 30 * s, y + 62 * s, x + 26 * s, y + 28 * s)
    closePath()
}

private fun outline(width: Double): BasicStroke =
    BasicStroke(width.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

/**
 * One flat countable, drawn exactly as the app draws it. [box] in pixels.
 * [keyline] adds a white sticker edge outside the outline, for art that
 * sits on the cream ground.
 */
fun flatCountable(dst: Img, kind: String, cx: Double, cy: Double, box: Double, keyline: Double = 0.0) {
    val s = box / 100.0
    val x = cx - box / 2
    val y = cy - box / 2
    val c = when (kind) {
        "apple" -> APPLE_C
        "star" -> STAR_C
        "carrot" -> CARROT_C
        else -> BALL_C
    }
    val body = when (kind) {
        "apple" -> applePath(x, y, s)
        "star" -> starPath(x, y, s)
        "carrot" -> carrotPath(x, y, s)
        else -> Ellipse2D.Double(x + 6 * s, y + 6 * s, 88 * s, 88 * s)
    }
    val g = graphics(dst)
    g.stroke = outline(4 * s)
    if (kind == "carrot") {
        // The green fronds sit behind the body.
        for (fx in listOf(-26.0, 0.0, 26.0)) {
            capsule(g, x + 50 * s, y + 30 * s, x + (50 + fx) * s, y + 4 * s, 2.7 * s, rgb(51, 168, 82))
        }
    }
    if (kind == "apple") {
        // Stem and leaf sit behind the body outline.
        capsule(g, x + 50 * s, y + 26 * s, x + 56 * s, y + 6 * s, 2.5 * s, STEM_BROWN)
        g.argb(rgb(51, 168, 82))
        g.fill(leafPath(x, y, s))
        g.argb(LEAF_DARK)
        g.stroke = outline(2.6 * s)
        g.draw(leafPath(x, y, s))
        g.stroke = outline(4 * s)
    }
    g.argb(c.fill)
    g.fill(body)
    val oldClip = g.clip
    g.clip = body
    g.argb(c.facet)
    g.fill(facetPath(x, y, s))
    g.clip = oldClip
    if (kind == "ball") {
        // The two liner bands, clipped to the ball.
        g.clip = body
        g.argb(LINER)
        g.stroke = BasicStroke((9 * s).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)
        g.draw(Path2D.Double().apply {
            moveTo(x + 9 * s, y + 40 * s)
            quadTo(x + 50 * s, y + 30 * s, x + 91 * s, y + 40 * s)
        })
        g.draw(Path2D.Double().apply {
            moveTo(x + 12 * s, y + 66 * s)
            quadTo(x + 50 * s, y + 76 * s, x + 88 * s, y + 66 * s)
        })
        g.clip = oldClip
        g.stroke = outline(4 * s)
    }
    if (keyline > 0) {
        g.argb(WHITE)
        g.stroke = outline(4 * s + keyline * 2)
        g.draw(body)
    }
    g.argb(c.stroke)
    g.stroke = outline(4 * s)
    g.draw(body)
    g.dispose()
}

/** The app's counting chip: navy disc, white numeral in the brand face. */
fun flatChip(dst: Img, cx: Double, cy: Double, dia: Double, text: String) {
    val g = graphics(dst)
    g.argb(CHIP_BLUE)
    g.fill(Ellipse2D.Double(cx - dia / 2, cy - dia / 2, dia, dia))
    g.dispose()
    plainText(dst, cx, cy - dia * 0.04, text, "black", (dia * 0.62).toInt(), WHITE)
}

/** A rounded seat disc, the bowl's part colours. */
fun flatSeat(dst: Img, cx: Double, cy: Double, dia: Double, seat: Int) {
    val g = graphics(dst)
    g.argb(seat)
    g.fill(Ellipse2D.Double(cx - dia / 2, cy - dia / 2, dia, dia))
    g.dispose()
}

/** Sticker type in the bundled brand face: soft drop, white keyline, fill. */
fun brandText(
    dst: Img,
    x: Double,
    y: Double,
    text: String,
    size: Int,
    fill: Int,
    keyline: Double = 0.0,
    keylineFill: Int = WHITE,
    shadow: Quad = Quad(0.0, size * 0.06, 45, size * 0.035),
    anchor: String = "mm",
) {
    val f = font("black", size)
    val (dx, base) = anchorPos(text, f, x, y, anchor)

    val lay = img(dst.width, dst.height)
    val lg = graphicsText(lay)
    drawGlyphs(lg, text, f, dx + shadow.a, base + shadow.b, INK or (shadow.c shl 24), 0, INK)
    lg.dispose()
    val blurred = gaussianBlur(lay, shadow.d)
    val g = graphicsText(dst)
    g.drawImage(blurred, 0, 0, null)
    if (keyline > 0) {
        drawGlyphs(g, text, f, dx, base, fill, keyline.toInt(), keylineFill)
    }
    drawGlyphs(g, text, f, dx, base, fill, 0, fill)
    g.dispose()
}

/** A numeral with two keylines, the way the in-app shapes wear their outlines. */
fun outlinedNumeral(
    dst: Img,
    x: Double,
    y: Double,
    text: String,
    size: Int,
    fill: Int,
    outlineColor: Int,
    shadow: Quad = Quad(0.0, size * 0.06, 50, size * 0.04),
) {
    val f = font("black", size)
    val (dx, base) = anchorPos(text, f, x, y, "mm")

    val lay = img(dst.width, dst.height)
    val lg = graphicsText(lay)
    drawGlyphs(lay.let { lg }, text, f, dx + shadow.a, base + shadow.b, INK or (shadow.c shl 24), 0, INK)
    lg.dispose()
    val blurred = gaussianBlur(lay, shadow.d)
    val g = graphicsText(dst)
    g.drawImage(blurred, 0, 0, null)
    drawGlyphs(g, text, f, dx, base, fill, (size * 0.16).toInt(), WHITE)
    drawGlyphs(g, text, f, dx, base, fill, (size * 0.07).toInt(), outlineColor)
    drawGlyphs(g, text, f, dx, base, fill, 0, fill)
    g.dispose()
}
