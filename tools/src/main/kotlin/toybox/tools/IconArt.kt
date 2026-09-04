package toybox.tools

import java.awt.BasicStroke
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.RoundRectangle2D
import java.io.File

/*
 * The icon is the app's own central object: the poured bowl, the one place
 * where addition is visible as a fact. In the calm gallery the tray is a
 * white card floating on soft light, the candy green edge peeking beneath
 * it, exactly the keycap move the app's own keys make. Five countables sit
 * in their part colours, three apples on blue seats above two balls on
 * orange seats, the 3+2 the app itself packs. One silhouette, nothing
 * overlapping, no numerals: quantities, not symbols, which is the promise
 * the app makes a three-year-old. The shapes are the exact bodies the
 * Compose UI draws, so the icon and the first screen speak the same
 * language. Any mask or any size crops only ground.
 */

private val GROUND = rgb(246, 242, 234)
private val GROUND_DEEP = rgb(237, 231, 219)

/** The warm paper ground, gently lit, exactly like the app and the feature. */
fun iconBg(size: Int): Img {
    val img = vgrad(size, size, listOf(0.0 to GROUND, 1.0 to GROUND_DEEP))
    cornerShade(img, 12.0)
    return img
}

/** A soft blurred warm ellipse under the card, so it sits on the ground, not on it. */
private fun softShadow(dst: Img, cx: Double, cy: Double, rx: Double, ry: Double, alpha: Int, sigma: Double) {
    val pad = sigma.toInt() * 3 + 2
    val tile = img((rx * 2).toInt() + pad * 2, (ry * 2).toInt() + pad * 2)
    val g = graphics(tile)
    g.argb((alpha shl 24) or (SHADOW_WARM and 0xFFFFFF))
    g.fill(Ellipse2D.Double(pad.toDouble(), pad.toDouble(), rx * 2, ry * 2))
    g.dispose()
    placeTile(dst, gaussianBlur(tile, sigma), cx, cy)
}

/**
 * The gallery card: a white rounded card on a soft shadow, the candy edge
 * showing as a stripe beneath, one hairline holding the white against the
 * ground. The edge height scales with the card, as the keys' lift does.
 */
private fun galleryCard(img: Img, x: Double, y: Double, w: Double, h: Double, edge: Int) {
    val corner = minOf(w, h) * 0.14
    val edgeH = h * 0.05
    softShadow(img, x + w / 2, y + h + edgeH * 0.6, w * 0.46, h * 0.10, 52, maxOf(2.0, w * 0.018))
    val g = graphics(img)
    g.argb(edge)
    g.fill(RoundRectangle2D.Double(x, y + edgeH, w, h, corner, corner))
    g.argb(WHITE)
    g.fill(RoundRectangle2D.Double(x, y, w, h, corner, corner))
    g.argb((20 shl 24) or (INK and 0xFFFFFF))
    g.stroke = BasicStroke(maxOf(1.5, w * 0.003).toFloat())
    g.draw(RoundRectangle2D.Double(x, y, w, h, corner, corner))
    g.dispose()
}

/**
 * The bowl exactly as the app now draws it: a white gallery card with the
 * green edge beneath, five seats in two rows, the parts still wearing
 * their part colours. [w] is the card's outer width; every other
 * proportion follows it.
 */
fun paintBowl(img: Img, cx: Double, cy: Double, w: Double) {
    val h = w * 0.52
    galleryCard(img, cx - w / 2, cy - h / 2, w, h, RIM_GREEN)
    val seat = w * 0.205
    val box = seat * 0.86
    val rowDy = h * 0.16
    val colDx = w * 0.215
    // Three apples on blue seats above, two balls on orange seats below:
    // the parts keep their colours inside the whole, the app's own 3+2.
    for (t in listOf(-1.0, 0.0, 1.0)) {
        flatSeat(img, cx + t * colDx, cy - rowDy, seat, SEAT_A)
        flatCountable(img, "apple", cx + t * colDx, cy - rowDy, box)
    }
    for (t in listOf(-0.5, 0.5)) {
        flatSeat(img, cx + t * colDx, cy + rowDy, seat, SEAT_B)
        flatCountable(img, "ball", cx + t * colDx, cy + rowDy, box)
    }
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
    paintBowl(img, px / 2.0, px * 0.52, px * 0.80)
    roundCorners(img, 0.21)
    // No unsharp here: on the low-contrast ground its edge halo shows, and a
    // straight bicubic downscale of flat candy is already crisp.
    savePng(resizeBicubic(img, 512, 512), File(root, "play-store/play-icon-512.png"))
}

/**
 * The themed-icon glyph (Android 13+): the same poured bowl, reduced to one
 * white outline. The launcher tints it however the parent's wallpaper wants,
 * so it ships as alpha-only art. No seats: single-tone glyphs read best as
 * pure line, and the five shapes still say 3+2 by their arrangement.
 */
fun paintMonochrome(img: Img, cx: Double, cy: Double, w: Double) {
    val h = w * 0.52
    val g = graphics(img)
    val corner = minOf(w, h) * 0.14
    val stroke = maxOf(1.5, w * 0.016)
    g.argb(WHITE)
    g.stroke = BasicStroke(stroke.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.draw(RoundRectangle2D.Double(cx - w / 2, cy - h / 2, w, h, corner, corner))
    val seat = w * 0.205
    val box = seat * 0.86
    val rowDy = h * 0.16
    val colDx = w * 0.215
    for (t in listOf(-1.0, 0.0, 1.0)) {
        val ax = cx + t * colDx - box / 2
        val ay = cy - rowDy - box / 2
        g.draw(applePath(ax, ay, box / 100.0))
        // The stem, so the outline reads as an apple at any size.
        g.draw(Line2D.Double(ax + 50 * box / 100.0, ay + 26 * box / 100.0, ax + 56 * box / 100.0, ay + 6 * box / 100.0))
    }
    for (t in listOf(-0.5, 0.5)) {
        g.draw(Ellipse2D.Double(cx + t * colDx - box * 0.44, cy + rowDy - box * 0.44, box * 0.88, box * 0.88))
    }
    g.dispose()
}

private val DENSITIES = listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

fun launcherIcons(root: File) {
    for ((name, dp) in DENSITIES) {
        val legacy = iconBg(dp)
        paintBowl(legacy, dp * 0.5, dp * 0.52, dp * 0.90)
        roundCorners(legacy, 0.22)
        savePng(legacy, File(root, "app/src/main/res/mipmap-$name/ic_launcher.png"))

        // Adaptive foreground: full-bleed ground; the card stays inside the
        // 66/108 safe circle so any mask crops only ground.
        val canvas = (dp * 108 / 48)
        val fg = iconBg(canvas)
        paintBowl(fg, canvas / 2.0, canvas * 0.52, canvas * 0.54)
        savePng(fg, File(root, "app/src/main/res/mipmap-$name/ic_launcher_foreground.png"))

        // Themed-icon layer: same geometry, alpha-only white outline.
        val mono = img(canvas, canvas)
        paintMonochrome(mono, canvas / 2.0, canvas * 0.52, canvas * 0.54)
        savePng(mono, File(root, "app/src/main/res/mipmap-$name/ic_launcher_monochrome.png"))
    }
}
