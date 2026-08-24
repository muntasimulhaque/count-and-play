package toybox.tools

import java.io.File
import kotlin.random.Random

/*
 * The feature graphic is the home screen, staged for the store: a 2048 x
 * 1000 master downscaled to 1024 x 500. A title band on the warm cream,
 * then the three games as the app's own candy tiles, each showing its real
 * scene: counting with the chips on, the plates and the bowl with the parts
 * seated inside the whole, and take-away with its ghosts. The promise line
 * closes the band. No shadows anywhere: the white sticker keyline carries
 * the depth, exactly as it does in the app.
 */

private val CREAM = rgb(255, 246, 227)
private val CREAM_DEEP = rgb(255, 233, 189)
private val BLUE = rgb(28, 169, 232)
private val DEEP_BLUE = rgb(11, 114, 168)
private val RED = rgb(227, 59, 44)
private val TEAL = rgb(14, 160, 174)
private val YELLOW = rgb(250, 184, 5)
private val GREEN = rgb(51, 168, 82)
private val ORANGE = rgb(240, 106, 14)
private val PINK = rgb(236, 72, 153)
private val CONFETTI_COLORS = listOf(BLUE, RED, YELLOW, GREEN, TEAL, ORANGE, PINK)

/** No shadow: alpha zero keeps the sticker keyline as the only depth. */
private val NO_SHADOW = Quad(0.0, 0.0, 0, 0.0)

private fun featureGround(): Img {
    val w = 2048
    val h = 1000
    val img = vgrad(w, h, listOf(0.0 to CREAM, 1.0 to CREAM_DEEP))
    cornerShade(img, 12.0)
    val rng = Random(11)
    confetti(img, rng, 14, 60 to 1990, 30 to 120, CONFETTI_COLORS, amax = 36)
    return img
}

private fun featureBand(img: Img) {
    // One centred column: wordmark above, tagline below, tiles beneath.
    val w1 = textAdvance("Count", font("black", 160))
    val w2 = textAdvance("& Play", font("black", 160))
    val x = 1024.0 - (w1 + 42.0 + w2) / 2
    brandText(img, x, 168.0, "Count", 160, RED, keyline = 14.0, shadow = NO_SHADOW, anchor = "lm")
    brandText(img, x + w1 + 42.0, 168.0, "& Play", 160, BLUE, keyline = 14.0, shadow = NO_SHADOW, anchor = "lm")
    val tag = "See addition and subtraction happen"
    val ta = textAdvance(tag, font("bold", 54))
    brandText(img, 1024.0 - ta / 2, 288.0, tag, 54, DEEP_BLUE, shadow = NO_SHADOW, anchor = "lm")
}

/** One home tile: tinted liner, fat candy rim, scene inside. */
private fun tile(img: Img, x: Double, y: Double, w: Double, h: Double, rim: Int) {
    flatTray(img, x, y, w, h, rim, 22.0)
    // Tint the liner with the rim, the way the home keys wear their colour.
    val g = graphics(img)
    g.argb((36 shl 24) or (rim and 0xFFFFFF))
    val corner = minOf(w, h) * 0.22 - 22.0
    g.fill(
        java.awt.geom.RoundRectangle2D.Double(
            x + 22.0, y + 22.0, w - 44.0, h - 44.0,
            maxOf(1.0, corner), maxOf(1.0, corner),
        ),
    )
    g.dispose()
}

/** COUNT: three apples, chips in tapping order. */
private fun sceneCount(img: Img, cx: Double, cy: Double) {
    // Five apples in the app's own 3+2 arrangement, chips in tap order.
    val box = 150.0
    val top = listOf(-1.0, 0.0, 1.0)
    val bot = listOf(-0.5, 0.5)
    var n = 1
    for (t in top) {
        val x = cx + t * (box + 25.0)
        flatCountable(img, "apple", x, cy - box * 0.62, box, keyline = 8.0)
        flatChip(img, x + box * 0.42, cy - box * 0.62 - box * 0.42, box * 0.5, n.toString())
        n++
    }
    for (t in bot) {
        val x = cx + t * (box + 25.0)
        flatCountable(img, "apple", x, cy + box * 0.62, box, keyline = 8.0)
        flatChip(img, x + box * 0.42, cy + box * 0.62 - box * 0.42, box * 0.5, n.toString())
        n++
    }
}

/** ADD: two plates above, the bowl below with the parts seated inside. */
private fun sceneAdd(img: Img, cx: Double, cy: Double) {
    val pw = 262.0
    val ph = 170.0
    val py = cy - 235.0
    flatTray(img, cx - pw - 20.0, py, pw, ph, BLUE, 16.0)
    flatTray(img, cx + 20.0, py, pw, ph, ORANGE, 16.0)
    for (i in 0 until 2) flatCountable(img, "star", cx - pw - 20.0 + pw / 2 + (i - 0.5) * 100.0, py + ph / 2, 90.0, keyline = 6.0)
    for (i in 0 until 2) flatCountable(img, "ball", cx + 20.0 + pw / 2 + (i - 0.5) * 100.0, py + ph / 2, 90.0, keyline = 6.0)
    // The bowl: the two parts keep their seats inside the whole.
    val bw = 540.0
    val bh = 260.0
    val by = cy - 65.0
    flatTray(img, cx - bw / 2, by, bw, bh, GREEN, 16.0)
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
        flatCountable(img, "apple", cx + (i - 1.0) * step, cy - box * 0.62, box, keyline = 8.0)
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
    val rims = listOf(BLUE, GREEN, PINK)
    val scenes = listOf(::sceneCount, ::sceneAdd, ::sceneTake)
    for (i in 0 until 3) {
        tile(img, xs[i], y, w, h, rims[i])
        scenes[i](img, xs[i] + w / 2, y + h / 2)
    }
    val out = toRgb(resizeBicubic(img, 1024, 500))
    savePng(out, File(root, "play-store/feature-graphic-1024x500.png"))
}
