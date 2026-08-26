package toybox.tools

import java.awt.geom.RoundRectangle2D
import java.io.File
import kotlin.random.Random

/*
 * The feature graphic is the home screen, staged for the store: a 2048 x
 * 1000 master downscaled to 1024 x 500. A title band on the warm paper,
 * then the three games as the app's own gallery cards, each showing its
 * real scene: counting with the chips on, the plates and the bowl with the
 * parts seated inside the whole, and take-away with its ghosts. The cards
 * float on soft light with the candy edge peeking beneath, the keycap move
 * the app itself makes; color lives in the toys, the walls stay quiet.
 */

private val GROUND = rgb(246, 242, 234)
private val GROUND_DEEP = rgb(237, 231, 219)
private val BLUE = rgb(28, 169, 232)
private val RED = rgb(227, 59, 44)
private val TEAL = rgb(14, 160, 174)
private val YELLOW = rgb(250, 184, 5)
private val GREEN = rgb(51, 168, 82)
private val ORANGE = rgb(240, 106, 14)
private val PINK = rgb(236, 72, 153)
private val CONFETTI_COLORS = listOf(BLUE, RED, YELLOW, GREEN, TEAL, ORANGE, PINK)

/** The muted ink for the promise line, quiet against the paper. */
private val TAG_INK = mix(INK, GROUND, 0.35)

/** No shadow: alpha zero keeps the quiet surfaces as the only depth. */
private val NO_SHADOW = Quad(0.0, 0.0, 0, 0.0)

private fun featureGround(): Img {
    val w = 2048
    val h = 1000
    val img = vgrad(w, h, listOf(0.0 to GROUND, 1.0 to GROUND_DEEP))
    cornerShade(img, 12.0)
    val rng = Random(11)
    confetti(img, rng, 12, 60 to 1990, 30 to 130, CONFETTI_COLORS, amax = 30)
    return img
}

private fun featureBand(img: Img) {
    // One centred column: wordmark above, tagline below, tiles beneath. The
    // wordmark is ink: in the calm gallery the color lives in the toys.
    val w1 = textAdvance("Count", font("black", 160))
    val w2 = textAdvance("& Play", font("black", 160))
    val x = 1024.0 - (w1 + 42.0 + w2) / 2
    brandText(img, x, 168.0, "Count", 160, INK, anchor = "lm")
    brandText(img, x + w1 + 42.0, 168.0, "& Play", 160, INK, anchor = "lm")
    val tag = "See addition and subtraction happen"
    val ta = textAdvance(tag, font("bold", 54))
    brandText(img, 1024.0 - ta / 2, 288.0, tag, 54, TAG_INK, shadow = NO_SHADOW, anchor = "lm")
}

/** A soft blurred warm rounded-rect shadow under a floating card. */
private fun cardShadow(dst: Img, cx: Double, cy: Double, w: Double, h: Double, corner: Double, alpha: Int) {
    val sigma = 14.0
    val pad = sigma.toInt() * 3 + 2
    val tile = img(w.toInt() + pad * 2, h.toInt() + pad * 2)
    val g = graphics(tile)
    g.argb((alpha shl 24) or (SHADOW_WARM and 0xFFFFFF))
    g.fill(RoundRectangle2D.Double(pad.toDouble(), pad.toDouble(), w, h, corner, corner))
    g.dispose()
    placeTile(dst, gaussianBlur(tile, sigma), cx, cy)
}

private fun hairline(g: java.awt.Graphics2D, x: Double, y: Double, w: Double, h: Double, corner: Double) {
    g.argb((20 shl 24) or (INK and 0xFFFFFF))
    g.stroke = java.awt.BasicStroke(2.5f)
    g.draw(RoundRectangle2D.Double(x, y, w, h, corner, corner))
}

/** A plain white card on soft light, one hairline holding its edge. */
private fun plainCard(img: Img, x: Double, y: Double, w: Double, h: Double) {
    val corner = minOf(w, h) * 0.16
    cardShadow(img, x + w / 2, y + h * 0.98, w, h, corner, 46)
    val g = graphics(img)
    g.argb(WHITE)
    g.fill(RoundRectangle2D.Double(x, y, w, h, corner, corner))
    hairline(g, x, y, w, h, corner)
    g.dispose()
}

/** One home tile: a white card with the candy edge peeking beneath. */
private fun tile(img: Img, x: Double, y: Double, w: Double, h: Double, edge: Int) {
    val corner = minOf(w, h) * 0.16
    val edgeH = 18.0
    cardShadow(img, x + w / 2, y + h + edgeH * 0.5, w, h, corner, 52)
    val g = graphics(img)
    g.argb(edge)
    g.fill(RoundRectangle2D.Double(x, y + edgeH, w, h, corner, corner))
    g.argb(WHITE)
    g.fill(RoundRectangle2D.Double(x, y, w, h, corner, corner))
    hairline(g, x, y, w, h, corner)
    g.dispose()
}

/** COUNT: five apples in the 3+2, chips in tapping order. */
private fun sceneCount(img: Img, cx: Double, cy: Double) {
    val box = 150.0
    val top = listOf(-1.0, 0.0, 1.0)
    val bot = listOf(-0.5, 0.5)
    var n = 1
    for (t in top) {
        val x = cx + t * (box + 25.0)
        flatCountable(img, "apple", x, cy - box * 0.62, box)
        flatChip(img, x + box * 0.42, cy - box * 0.62 - box * 0.42, box * 0.5, n.toString())
        n++
    }
    for (t in bot) {
        val x = cx + t * (box + 25.0)
        flatCountable(img, "apple", x, cy + box * 0.62, box)
        flatChip(img, x + box * 0.42, cy + box * 0.62 - box * 0.42, box * 0.5, n.toString())
        n++
    }
}

/** ADD: two plates above, the bowl below with the parts seated inside. */
private fun sceneAdd(img: Img, cx: Double, cy: Double) {
    val pw = 262.0
    val ph = 170.0
    // The plates sit one edge-height higher, so the candy stripes they grow
    // end exactly where the bowl's card begins.
    val py = cy - 253.0
    tile(img, cx - pw - 20.0, py, pw, ph, BLUE)
    tile(img, cx + 20.0, py, pw, ph, ORANGE)
    for (i in 0 until 2) flatCountable(img, "star", cx - pw - 20.0 + pw / 2 + (i - 0.5) * 100.0, py + ph / 2, 90.0)
    for (i in 0 until 2) flatCountable(img, "ball", cx + 20.0 + pw / 2 + (i - 0.5) * 100.0, py + ph / 2, 90.0)
    // The bowl: the two parts keep their seats inside the whole.
    val bw = 540.0
    val bh = 260.0
    val by = cy - 65.0
    plainCard(img, cx - bw / 2, by, bw, bh)
    val box = 84.0
    val top = listOf(-1.0, 0.0, 1.0)
    val bot = listOf(-0.5, 0.5)
    for (t in top) {
        flatSeat(img, cx + t * (box + 22.0), by + bh / 2 - 48.0, box * 1.24, SEAT_A)
        flatCountable(img, "star", cx + t * (box + 22.0), by + bh / 2 - 48.0, box)
    }
    for (t in bot) {
        flatSeat(img, cx + t * (box + 22.0), by + bh / 2 + 48.0, box * 1.24, SEAT_B)
        flatCountable(img, "ball", cx + t * (box + 22.0), by + bh / 2 + 48.0, box)
    }
}

/** TAKE: two taken wear their numbers in their ghosts; three remain. */
private fun sceneTake(img: Img, cx: Double, cy: Double) {
    // Five slots, 3+2 like the app packs them: two taken wear their numbers
    // in their ghosts, three remain to be counted.
    val box = 140.0
    val step = box + 25.0
    for (i in 0 until 2) {
        val x = cx + (i - 0.5) * step
        ghostSlot(img, x, cy + box * 0.62, box)
        flatChip(img, x + box * 0.42, cy + box * 0.62 - box * 0.42, box * 0.5, (i + 1).toString())
    }
    for (i in 0 until 3) {
        flatCountable(img, "apple", cx + (i - 1.0) * step, cy - box * 0.62, box)
    }
}

/** The dashed ring an emptied slot wears in the app. */
private fun ghostSlot(img: Img, cx: Double, cy: Double, box: Double) {
    val g = graphics(img)
    g.argb(rgb(183, 192, 204))
    g.stroke = java.awt.BasicStroke(
        (box * 0.05).toFloat(),
        java.awt.BasicStroke.CAP_BUTT,
        java.awt.BasicStroke.JOIN_ROUND,
        10.0f,
        floatArrayOf((box * 0.11).toFloat(), (box * 0.09).toFloat()),
        0f,
    )
    g.draw(java.awt.geom.Ellipse2D.Double(cx - box * 0.42, cy - box * 0.42, box * 0.84, box * 0.84))
    g.dispose()
}

fun featureGraphic(root: File) {
    val img = featureGround()
    featureBand(img)
    val y = 330.0
    val h = 610.0
    val w = 608.0
    val xs = listOf(64.0, 720.0, 1376.0)
    val edges = listOf(BLUE, GREEN, PINK)
    val scenes = listOf(::sceneCount, ::sceneAdd, ::sceneTake)
    for (i in 0 until 3) {
        tile(img, xs[i], y, w, h, edges[i])
        scenes[i](img, xs[i] + w / 2, y + h / 2)
    }
    val out = toRgb(resizeBicubic(img, 1024, 500))
    savePng(out, File(root, "play-store/feature-graphic-1024x500.png"))
}
