package toybox.tools

import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * The icon scene lives in a 1024 design box: five balls packed two over
 * three, tangent to each other, centred on the canvas. Two and three make
 * five, and nothing else in the frame competes with that.
 */

private val BLUE = rgb(28, 169, 232)
private val BLUE_LIGHT = rgb(99, 203, 246)
private val BLUE_DEEP = rgb(8, 110, 180)
private val YELLOW = rgb(250, 184, 5)
private val RED = rgb(227, 59, 44)
private val ORANGE = rgb(240, 106, 14)
private val GREEN = rgb(51, 168, 82)
private val TEAL = rgb(14, 160, 174)

private val SPARKLES = listOf(
    Sparkle(168.0, 196.0, 30.0, WHITE, 170),
    Sparkle(872.0, 300.0, 22.0, YELLOW, 180),
    Sparkle(140.0, 800.0, 16.0, WHITE, 125),
)

/** Candy blue, lit from the top left and falling into deep blue. */
fun iconBg(size: Int): Img = rgrad(
    size, size,
    size * 0.36, size * 0.24, size * 0.75, size * 0.86,
    listOf(0.0 to BLUE_LIGHT, 0.55 to BLUE, 1.0 to BLUE_DEEP),
)

/** Five balls packed two over three, one pooled shadow beneath. */
fun paintScene(img: Img, cx: Double, cy: Double, s: Double, sparkles: Boolean = true) {
    val r = 134 * s
    val dy = 1.732 * r
    data class Seat(val x: Double, val y: Double, val color: Int)
    val seats = listOf(
        Seat(cx - r, cy - dy / 2, YELLOW), Seat(cx + r, cy - dy / 2, RED),
        Seat(cx - 2 * r, cy + dy / 2, ORANGE), Seat(cx, cy + dy / 2, GREEN),
        Seat(cx + 2 * r, cy + dy / 2, TEAL),
    )
    val base = dy / 2 + r * 1.27
    softEllipse(img, cx, cy + base, 3.3 * r, 0.65 * r, rgb(7, 74, 132), 120, 0.35 * r)
    softEllipse(img, cx, cy + base - 8 * s, 2.4 * r, 0.45 * r, rgb(6, 60, 110), 85, 0.17 * r)
    for (seat in seats) {
        placeTile(img, ballTile((r * 2).roundToInt(), seat.color), seat.x, seat.y)
    }
    if (sparkles) {
        for (sp in SPARKLES) {
            placeTile(img, star4(sp.x, sp.y, sp.r, sp.color, sp.alpha), sp.x, sp.y)
        }
    }
}

/** A quiet inner highlight along the rounded edge: weight without chrome. */
fun gelEdge(img: Img, ratio: Double = 0.205) {
    val px = img.width
    val bez = img(px, px)
    val g = graphics(bez)
    g.argb((85 shl 24) or (WHITE and 0xFFFFFF))
    g.stroke = java.awt.BasicStroke(max(3.0, px * 0.007).toFloat())
    // RoundRectangle2D takes the full arc diameter: double the corner radius.
    g.draw(RoundRectangle2D.Double(px * 0.014, px * 0.014, px * 0.972, px * 0.972, px * ratio * 2, px * ratio * 2))
    g.dispose()
    placeTile(gaussianBlur(bez, max(2.0, px * 0.003)), img, 0.0, 0.0)
}

/** Keep pixels only inside a rounded square of [ratio] corner radius. */
fun roundCorners(img: Img, ratio: Double) {
    val w = img.width
    val h = img.height
    val rr = RoundRectangle2D.Double(0.0, 0.0, (w - 1).toDouble(), (h - 1).toDouble(), w * ratio * 2, h * ratio * 2)
    val alpha = img.alphaRaster
    for (y in 0 until h) for (x in 0 until w) {
        if (!rr.contains(x + 0.5, y + 0.5)) alpha.setPixel(x, y, intArrayOf(0))
    }
}

fun storeIcon(root: File) {
    val px = 1024
    val img = iconBg(px)
    paintScene(img, px / 2.0, px / 2.0, 1.0)
    gelEdge(img)
    roundCorners(img, 0.21)
    val out = unsharp(resizeBicubic(img, 512, 512), radius = 1.5, percent = 0.50, threshold = 2.0)
    savePng(out, File(root, "play-store/play-icon-512.png"))
}

private val DENSITIES = listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

fun launcherIcons(root: File) {
    for ((name, dp) in DENSITIES) {
        val legacy = iconBg(dp)
        paintScene(legacy, dp * 0.5, dp * 0.5, dp / 1024.0 * 1.06, sparkles = dp >= 96)
        gelEdge(legacy, ratio = 0.215)
        roundCorners(legacy, 0.22)
        savePng(legacy, File(root, "app/src/main/res/mipmap-$name/ic_launcher.png"))

        // Adaptive foreground: full-bleed gradient; the scene must survive any
        // mask, so it keeps inside the 66/108 safe circle.
        val canvas = (dp * 108 / 48)
        val fg = iconBg(canvas)
        val safeR = canvas * 66 / 108 / 2.0
        paintScene(fg, canvas / 2.0, canvas / 2.0, safeR * 0.94 / 474, sparkles = false)
        savePng(fg, File(root, "app/src/main/res/mipmap-$name/ic_launcher_foreground.png"))
    }
}
