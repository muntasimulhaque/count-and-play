package toybox.tools

import java.awt.geom.Ellipse2D
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.random.Random

/*
 * The feature graphic, staged on a 2048 x 1000 master and downscaled to the
 * store's 1024 x 500: title on a warm cream stage with confetti, and the
 * app's money shot as hero, two red balls plus three teal ones equalling a
 * giant glossy five.
 */

private val CREAM = rgb(255, 246, 227)
private val CREAM_DEEP = rgb(255, 233, 189)
private val BLUE = rgb(28, 169, 232)
private val CHIP_BLUE = rgb(39, 53, 122)
private val RED = rgb(227, 59, 44)
private val ORANGE = rgb(240, 106, 14)
private val YELLOW = rgb(250, 184, 5)
private val GREEN = rgb(51, 168, 82)
private val TEAL = rgb(14, 160, 174)
private val CONFETTI_COLORS = listOf(BLUE, RED, YELLOW, GREEN, TEAL, ORANGE)

private fun featureGround(): Img {
    val w = 2048
    val h = 1000
    val img = vgrad(w, h, listOf(0.0 to CREAM, 1.0 to CREAM_DEEP))
    softEllipse(img, 300.0, 130.0, 260.0, 200.0, WHITE, 26, 90.0)
    softEllipse(img, 1960.0, 900.0, 300.0, 240.0, WHITE, 22, 100.0)
    cornerShade(img, 26.0)
    val rng = Random(11)
    confetti(img, rng, 14, 60 to 1990, 40 to 150, CONFETTI_COLORS, amax = 58)
    confetti(img, rng, 18, 1080 to 2010, 80 to 950, CONFETTI_COLORS, amax = 64)
    confetti(img, rng, 10, 60 to 1000, 880 to 970, CONFETTI_COLORS, amax = 46)
    return img
}

private fun featureTitle(img: Img) {
    val x = 112.0
    var size = 196
    while (size > 20 && textAdvance("Count", font("black", size)) > 700) size -= 4
    for ((i, word) in listOf("Count", "& Play").withIndex()) {
        val cy = 320.0 + i * 208
        stickerText(img, x, cy, word, "black", size, INK, shadow = Quad(6.0, 18.0, 55, 10.0), anchor = "lm")
        val bb = textInkBounds(word, font("black", size), x, cy, "lm")
        val barColor = if (i == 0) RED else TEAL
        val g = graphics(img)
        capsule(g, bb.minX, bb.maxY + 30, bb.maxX, bb.maxY + 30, 13.0, barColor)
        g.dispose()
    }
    val g = graphics(img)
    g.argb((225 shl 24) or (INK and 0xFFFFFF))
    g.font = font("bold", 50)
    val frc = g.fontRenderContext
    val layout = java.awt.font.TextLayout("See addition and subtraction happen", g.font, frc)
    val base = 766.0 + (layout.ascent - layout.descent) / 2.0
    layout.draw(g, (x + 2).toFloat(), base.toFloat())
    g.dispose()
}

/** The game's counting chip: navy, white numeral, tiny drop. */
private fun countChip(img: Img, cx: Double, cy: Double, dia: Double, text: String) {
    softEllipse(img, cx + dia * 0.05, cy + dia * 0.12, dia * 0.52, dia * 0.38, SHADOW_WARM, 80, dia * 0.10)
    val g = graphics(img)
    g.argb(CHIP_BLUE)
    g.fill(Ellipse2D.Double(cx - dia / 2, cy - dia / 2, dia, dia))
    g.dispose()
    plainText(img, cx, cy - dia * 0.04, text, "black", (dia * 0.58).toInt(), WHITE)
}

private fun heroBall(img: Img, cx: Double, cy: Double, r: Double, color: Int) {
    softEllipse(img, cx, cy + r * 0.95, r * 1.02, r * 0.30, SHADOW_WARM, 65, r * 0.16)
    placeTile(img, ballTile((r * 2).toInt(), color), cx, cy)
}

private fun plusSign(img: Img, cx: Double, cy: Double, armR: Double, t: Double, color: Int) {
    softEllipse(img, cx + 5, cy + t * 0.35, armR * 1.35, armR * 0.95, SHADOW_WARM, 60, t * 0.85)
    val g = graphics(img)
    capsule(g, cx - armR, cy, cx + armR, cy, t / 2, color)
    capsule(g, cx, cy - armR, cx, cy + armR, t / 2, color)
    g.dispose()
}

private fun equalsSign(img: Img, cx: Double, cy: Double, w: Double, t: Double, gap: Double, color: Int) {
    softEllipse(img, cx + 5, cy + t * 0.4, w * 0.72, gap * 0.95, SHADOW_WARM, 55, t * 0.9)
    val g = graphics(img)
    capsule(g, cx - w / 2, cy - gap / 2, cx + w / 2, cy - gap / 2, t / 2, color)
    capsule(g, cx - w / 2, cy + gap / 2, cx + w / 2, cy + gap / 2, t / 2, color)
    g.dispose()
}

private fun featureHero(img: Img) {
    val rowY = 330.0
    val r = 68.0
    val reds = listOf(1164.0, 1314.0)
    val teals = listOf(1586.0, 1736.0, 1886.0)
    for ((n, x) in reds.withIndex()) {
        heroBall(img, x, rowY, r, RED)
        countChip(img, x + r * 0.62, rowY - r * 0.62, 88.0, (n + 1).toString())
    }
    plusSign(img, 1450.0, rowY, 56.0, 34.0, BLUE)
    for ((n, x) in teals.withIndex()) {
        heroBall(img, x, rowY, r, TEAL)
        countChip(img, x + r * 0.62, rowY - r * 0.62, 88.0, (n + 1).toString())
    }

    equalsSign(img, 1400.0, 660.0, 130.0, 34.0, 92.0, BLUE)
    stickerText(
        img, 1630.0, 654.0, "5", "black", 360, RED, stroke = 18,
        shadow = Quad(8.0, 24.0, 70, 13.0),
        tint = darken(RED, 0.32), tintOff = 13.0 to 15.0,
    )

    val sparkles = listOf(
        Sparkle(1100.0, 148.0, 26.0, WHITE, 165),
        Sparkle(1956.0, 500.0, 20.0, YELLOW, 185),
        Sparkle(1200.0, 852.0, 17.0, WHITE, 120),
    )
    for (sp in sparkles) placeTile(img, star4(sp.x, sp.y, sp.r, sp.color, sp.alpha), sp.x, sp.y)
}

fun featureGraphic(root: File) {
    val img = featureGround()
    featureTitle(img)
    featureHero(img)
    val out = unsharp(toRgb(resizeBicubic(img, 1024, 500)), radius = 1.8, percent = 0.55, threshold = 2.0)
    savePng(out, File(root, "play-store/feature-graphic-1024x500.png"))
}
