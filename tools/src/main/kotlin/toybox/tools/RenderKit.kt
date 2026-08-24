package toybox.tools

import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** An RGBA canvas; every generator draws with Java2D on these. */
typealias Img = java.awt.image.BufferedImage

fun img(w: Int, h: Int): Img = Img(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)

// -- The shared palette (verbatim from ui/Theme.kt and the art redesign) ------

val INK = rgb(34, 38, 46)
val SHADOW_WARM = rgb(150, 112, 58)
val WHITE = rgb(255, 255, 255)

fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

fun mix(a: Int, b: Int, t: Double): Int {
    fun ch(v: Int, shift: Int) = (v shr shift) and 0xFF
    var out = 0xFF shl 24
    for (shift in listOf(16, 8, 0)) {
        out = out or ((ch(a, shift) + ((ch(b, shift) - ch(a, shift)) * t).roundToInt()) shl shift)
    }
    return out
}

fun lighten(c: Int, t: Double) = mix(c, WHITE, t)
fun darken(c: Int, t: Double) = mix(c, rgb(0, 0, 0), t)

// -- Gradients -----------------------------------------------------------------

/** Vertical multi-stop gradient: stops = (position 0..1, opaque colour). */
fun vgrad(w: Int, h: Int, stops: List<Pair<Double, Int>>): Img {
    val out = img(w, h)
    val raster = out.raster
    val px = IntArray(w)
    val row = IntArray(w * 4)
    for (y in 0 until h) {
        interp(y.toDouble() / maxOf(1, h - 1), stops, px)
        for (x in 0 until w) {
            row[x * 4] = (px[x] shr 16) and 0xFF
            row[x * 4 + 1] = (px[x] shr 8) and 0xFF
            row[x * 4 + 2] = px[x] and 0xFF
            row[x * 4 + 3] = 255
        }
        raster.setPixels(0, y, w, 1, row)
    }
    return out
}

/** Elliptical radial gradient: t = 0 at the centre, 1 at the ellipse edge. */
fun rgrad(w: Int, h: Int, cx: Double, cy: Double, rx: Double, ry: Double, stops: List<Pair<Double, Int>>): Img {
    val out = img(w, h)
    val px = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val dx = (x - cx) / rx
            val dy = (y - cy) / ry
            val t = sqrt(dx * dx + dy * dy).coerceIn(0.0, 1.0)
            px[y * w + x] = interpOne(t, stops)
        }
    }
    out.raster.setDataElements(0, 0, w, h, px)
    return out
}

private fun interp(t: Double, stops: List<Pair<Double, Int>>, into: IntArray) {
    for (x in into.indices) into[x] = interpOne(t.coerceIn(0.0, 1.0), stops)
}

private fun interpOne(t: Double, stops: List<Pair<Double, Int>>): Int {
    if (t <= stops.first().first) return stops.first().second
    for (i in 0 until stops.size - 1) {
        val (p0, c0) = stops[i]
        val (p1, c1) = stops[i + 1]
        if (t >= p0 && t <= p1) {
            val u = ((t - p0) / maxOf(p1 - p0, 1e-9)).coerceIn(0.0, 1.0)
            return mix(c0, c1, u)
        }
    }
    return stops.last().second
}

// -- Blur ------------------------------------------------------------------------

/**
 * A three-pass box blur approximating a Gaussian of [sigma], the same trick
 * Pillow uses, so soft shadows and sheens keep the same spread. Straight
 * alpha throughout: channels blur independently, as PIL does.
 */
fun gaussianBlur(src: Img, sigma: Double): Img {
    if (sigma < 0.55) return src
    var cur = src
    for (box in boxesForGaussian(sigma, 3)) {
        cur = boxBlur(cur, (floor((box - 1) / 2) + 0.5).roundToInt())
    }
    return cur
}

private fun boxesForGaussian(sigma: Double, n: Int): List<Double> {
    val wIdeal = sqrt(12.0 * sigma * sigma / n + 1.0)
    var wl = floor(wIdeal)
    if (wl % 2 == 0.0) wl--
    val wu = wl + 2
    val mIdeal = (12.0 * sigma * sigma - n * wl * wl - 4 * n * wl - 3 * n) / (-4 * wl - 4)
    val m = mIdeal.roundToInt()
    return (0 until n).map { if (it < m) wl else wu }
}

private fun boxBlur(src: Img, r: Int): Img {
    if (r < 1) return src
    val w = src.width
    val h = src.height
    val a = src.getRGB(0, 0, w, h, null, 0, w)
    val tmp = IntArray(a.size)
    boxBlurH(a, tmp, w, h, r)
    boxBlurV(tmp, a, w, h, r)
    val out = img(w, h)
    out.setRGB(0, 0, w, h, a, 0, w)
    return out
}

private fun boxBlurH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
    val win = r * 2 + 1
    for (y in 0 until h) {
        var sr = 0; var sg = 0; var sb = 0; var sa = 0
        for (i in -r..r) {
            val p = src[y * w + i.coerceIn(0, w - 1)]
            sa += (p ushr 24) and 0xFF; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (x in 0 until w) {
            dst[y * w + x] = pack(sr / win, sg / win, sb / win, sa / win)
            val add = src[y * w + (x + r + 1).coerceAtMost(w - 1)]
            val sub = src[y * w + (x - r).coerceAtLeast(0)]
            sa += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
            sb += (add and 0xFF) - (sub and 0xFF)
        }
    }
}

private fun boxBlurV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
    val win = r * 2 + 1
    for (x in 0 until w) {
        var sr = 0; var sg = 0; var sb = 0; var sa = 0
        for (i in -r..r) {
            val p = src[i.coerceIn(0, h - 1) * w + x]
            sa += (p ushr 24) and 0xFF; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (y in 0 until h) {
            dst[y * w + x] = pack(sr / win, sg / win, sb / win, sa / win)
            val add = src[(y + r + 1).coerceAtMost(h - 1) * w + x]
            val sub = src[(y - r).coerceAtLeast(0) * w + x]
            sa += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
            sb += (add and 0xFF) - (sub and 0xFF)
        }
    }
}

private fun pack(r: Int, g: Int, b: Int, a: Int): Int =
    (a.coerceIn(0, 255) shl 24) or ((r.coerceIn(0, 255)) shl 16) or ((g.coerceIn(0, 255)) shl 8) or b.coerceIn(0, 255)

// -- Drawing helpers -----------------------------------------------------------

fun Graphics2D.argb(color: Int) {
    this.color = java.awt.Color(color, true)
}

fun graphics(im: Img): Graphics2D = im.createGraphics().apply {
    // Flat candy means hard facets and no blur, not jagged pixels: edges are
    // anti-aliased, exactly as Compose draws them on the device.
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
}

/** Composite [tile] centred on ([cx], [cy]). */
fun placeTile(dst: Img, tile: Img, cx: Double, cy: Double) {
    val g = graphics(dst)
    g.drawImage(tile, (cx - tile.width / 2.0).roundToInt(), (cy - tile.height / 2.0).roundToInt(), null)
    g.dispose()
}

/** A stadium-shaped stroke: line with round caps. */
fun capsule(g: Graphics2D, x0: Double, y0: Double, x1: Double, y1: Double, r: Double, fill: Int) {
    g.argb(fill)
    g.stroke = BasicStroke((r * 2).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.draw(Line2D.Double(x0, y0, x1, y1))
}

/** Scatter small circles, pills and triangles, gently rotated. */
fun confetti(
    dst: Img,
    rng: Random,
    n: Int,
    xbox: Pair<Int, Int>,
    ybox: Pair<Int, Int>,
    colors: List<Int>,
    amax: Int = 60,
    smin: Double = 9.0,
    smax: Double = 24.0,
) {
    repeat(n) {
        val s = rng.nextDouble(smin, smax)
        val c = colors[rng.nextInt(colors.size)]
        val a = rng.nextInt(26, amax + 1)
        val padv = (s * 2.6).toInt()
        val tile = img(padv, padv)
        val g = graphics(tile)
        g.argb((a shl 24) or (c and 0xFFFFFF))
        val mid = padv / 2.0
        when (rng.nextDouble()) {
            in 0.0..<0.35 -> g.fill(Ellipse2D.Double(mid - s / 2, mid - s / 2, s, s))
            in 0.35..<0.70 -> {
                val rr = RoundRectangle2D.Double(mid - s * 0.8, mid - s * 0.3, s * 1.6, s * 0.6, s * 0.6, s * 0.6)
                g.fill(rr)
            }
            else -> {
                val tri = Path2D.Double()
                tri.moveTo(mid, mid - s * 0.65)
                tri.lineTo(mid + s * 0.62, mid + s * 0.5)
                tri.lineTo(mid - s * 0.62, mid + s * 0.5)
                tri.closePath()
                g.fill(tri)
            }
        }
        g.dispose()
        val rot = AffineTransform.getRotateInstance(rng.nextDouble(0.0, 2.0 * Math.PI), padv / 2.0, padv / 2.0)
        val op = java.awt.image.AffineTransformOp(rot, java.awt.image.AffineTransformOp.TYPE_BILINEAR)
        val rotated = op.filter(tile, null)
        placeTile(dst, rotated, rng.nextInt(xbox.first, xbox.second + 1).toDouble(),
            rng.nextInt(ybox.first, ybox.second + 1).toDouble())
    }
}

/** Gently darken toward the corners so the ground has depth. */
fun cornerShade(dst: Img, alpha: Double, shade: Int = rgb(24, 18, 10)) {    val w = dst.width
    val h = dst.height
    val g = graphics(dst)
    val step = 4
    for (by in 0 until h step step) {
        for (bx in 0 until w step step) {
            val x = bx + step / 2.0
            val y = by + step / 2.0
            val t = sqrt(((x - w / 2) / (w * 0.72)).pow2() + ((y - h / 2) / (h * 0.80)).pow2())
            val a = (((t - 0.55).coerceIn(0.0, 1.0) / 0.45) * alpha).roundToInt().coerceIn(0, 255)
            if (a > 0) {
                g.argb((a shl 24) or (shade and 0xFFFFFF))
                g.fillRect(bx, by, step, step)
            }
        }
    }
    g.dispose()
}

internal fun Double.pow2(): Double = this * this

// -- Output pipeline ------------------------------------------------------------

fun resizeBicubic(src: Img, w: Int, h: Int): Img {
    val out = img(w, h)
    val g = graphics(out)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(src, 0, 0, w, h, null)
    g.dispose()
    return out
}

/** Drop the alpha channel for formats that must ship opaque. */
fun toRgb(src: Img): Img {
    val out = Img(src.width, src.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    g.drawImage(src, 0, 0, null)
    g.dispose()
    return out
}

/** Pillow-style unsharp mask: add a fraction of the high-pass where it clears [threshold]. */
fun unsharp(src: Img, radius: Double, percent: Double, threshold: Double): Img {
    val blurred = gaussianBlur(src, radius)
    val w = src.width
    val h = src.height
    val a = src.getRGB(0, 0, w, h, null, 0, w)
    val b = blurred.getRGB(0, 0, w, h, null, 0, w)
    val out = img(w, h)
    val px = IntArray(w * h)
    for (i in a.indices) {
        var r = (a[i] shr 16) and 0xFF; var g = (a[i] shr 8) and 0xFF; var bl = a[i] and 0xFF
        val channels = intArrayOf(r, g, bl)
        for (ch in 0 until 3) {
            val diff = channels[ch] - channelAt(b[i], ch)
            if (abs(diff) > threshold) channels[ch] = (channels[ch] + diff * percent).roundToInt().coerceIn(0, 255)
        }
        r = channels[0]; g = channels[1]; bl = channels[2]
        px[i] = (a[i] and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or bl
    }
    out.raster.setPixels(0, 0, w, h, argbFrom(px))
    return out
}

private fun channelAt(argb: Int, ch: Int): Int = when (ch) {
    0 -> (argb shr 16) and 0xFF
    1 -> (argb shr 8) and 0xFF
    else -> argb and 0xFF
}

private fun argbFrom(px: IntArray): IntArray {
    val rows = IntArray(px.size * 4)
    for (i in px.indices) {
        rows[i * 4] = (px[i] shr 16) and 0xFF
        rows[i * 4 + 1] = (px[i] shr 8) and 0xFF
        rows[i * 4 + 2] = px[i] and 0xFF
        rows[i * 4 + 3] = (px[i] shr 24) and 0xFF
    }
    return rows
}

fun savePng(img: Img, file: File) {
    file.parentFile?.mkdirs()
    ImageIO.write(img, "png", file)
    println(file.absolutePath)
}

/** Shared sparkle record for the icon shelf and the feature hero. */
internal data class Sparkle(val x: Double, val y: Double, val r: Double, val color: Int, val alpha: Int)

/** Plain text, no chrome: the counting chips' white numerals. */
fun plainText(dst: Img, x: Double, y: Double, text: String, kind: String, size: Int, fill: Int, anchor: String = "mm") {
    val f = font(kind, size)
    val (dx, baseline) = anchorPos(text, f, x, y, anchor)
    val g = graphicsText(dst)
    drawGlyphs(g, text, f, dx, baseline, fill, 0, fill)
    g.dispose()
}
