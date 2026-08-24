package toybox.tools

import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.io.File

/*
 * The icon is the app's own central object: the poured bowl, the one place
 * where addition is visible as a fact. The app's green tray-bowl sits on the
 * warm cream ground, five countables still seated in their part colours,
 * three apples on blue seats above two balls on orange seats, the 3+2 the
 * app itself packs. One silhouette, nothing overlapping, no numerals:
 * quantities, not symbols, which is the promise the app makes a
 * three-year-old. The shapes are the exact bodies the Compose UI draws, so
 * the icon and the first screen speak the same language. Any mask or any
 * size crops only cream.
 */

private val CREAM = rgb(255, 246, 227)
private val CREAM_DEEP = rgb(255, 233, 189)

/** The warm paper ground, gently lit, exactly like the app and the feature. */
fun iconBg(size: Int): Img {
    val img = vgrad(size, size, listOf(0.0 to CREAM, 1.0 to CREAM_DEEP))
    cornerShade(img, 12.0)
    return img
}

/** A soft blurred ellipse under the bowl, so it sits on the ground, not on it. */
private fun softShadow(dst: Img, cx: Double, cy: Double, rx: Double, ry: Double, alpha: Int, sigma: Double) {
    val pad = sigma.toInt() * 3 + 2
    val tile = img((rx * 2).toInt() + pad * 2, (ry * 2).toInt() + pad * 2)
    val g = graphics(tile)
    g.argb((alpha shl 24) or (INK and 0xFFFFFF))
    g.fill(Ellipse2D.Double(pad.toDouble(), pad.toDouble(), rx * 2, ry * 2))
    g.dispose()
    placeTile(dst, gaussianBlur(tile, sigma), cx, cy)
}

/**
 * The bowl exactly as the app draws it: green tray, white liner, five seats
 * in two rows, the parts still wearing their part colours. [w] is the
 * bowl's outer width; every other proportion follows it.
 */
fun paintBowl(img: Img, cx: Double, cy: Double, w: Double) {
    val h = w * 0.52
    val rim = w * 0.068
    softShadow(img, cx, cy + h * 0.48, w * 0.46, h * 0.15, 30, maxOf(0.6, w * 0.02))
    flatTray(img, cx - w / 2, cy - h / 2, w, h, RIM_GREEN, rim)
    val seat = w * 0.205
    val box = seat * 0.86
    val rowDy = h * 0.16
    val colDx = w * 0.215
    // Three apples on blue seats above, two balls on orange seats below:
    // the parts keep their colours inside the whole, the app's own 3+2.
    for (t in listOf(-1.0, 0.0, 1.0)) {
        flatSeat(img, cx + t * colDx, cy - rowDy, seat, SEAT_A)
        flatCountable(img, "apple", cx + t * colDx, cy - rowDy, box, keyline = 5.0)
    }
    for (t in listOf(-0.5, 0.5)) {
        flatSeat(img, cx + t * colDx, cy + rowDy, seat, SEAT_B)
        flatCountable(img, "ball", cx + t * colDx, cy + rowDy, box, keyline = 5.0)
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
    // No unsharp here: on the low-contrast cream its edge halo shows, and a
    // straight bicubic downscale of flat candy is already crisp.
    savePng(resizeBicubic(img, 512, 512), File(root, "play-store/play-icon-512.png"))
}

private val DENSITIES = listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

fun launcherIcons(root: File) {
    for ((name, dp) in DENSITIES) {
        val legacy = iconBg(dp)
        paintBowl(legacy, dp * 0.5, dp * 0.52, dp * 0.90)
        roundCorners(legacy, 0.22)
        savePng(legacy, File(root, "app/src/main/res/mipmap-$name/ic_launcher.png"))

        // Adaptive foreground: full-bleed cream; the bowl stays inside the
        // 66/108 safe circle so any mask crops only ground.
        val canvas = (dp * 108 / 48)
        val fg = iconBg(canvas)
        paintBowl(fg, canvas / 2.0, canvas * 0.52, canvas * 0.54)
        savePng(fg, File(root, "app/src/main/res/mipmap-$name/ic_launcher_foreground.png"))
    }
}
