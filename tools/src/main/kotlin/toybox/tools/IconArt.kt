package toybox.tools

import java.awt.geom.RoundRectangle2D
import java.io.File

/*
 * The icon stands on the app's own warm cream, the one ground no other kids'
 * app dares on a shelf of saturated blues, so it reads as calm and warm at a
 * glance. On it, the game's atomic moment: shapes wearing their counting
 * chips, the numerals big enough to be the point. Apple one, star two, ball
 * three, clustered tight so any mask or any size crops only cream.
 * No sparkles, no gloss, no chrome, no black.
 */

private val CREAM = rgb(255, 246, 227)
private val CREAM_DEEP = rgb(255, 233, 189)

/** The warm paper ground, gently lit, exactly like the app and the feature. */
fun iconBg(size: Int): Img {
    val img = vgrad(size, size, listOf(0.0 to CREAM, 1.0 to CREAM_DEEP))
    cornerShade(img, 12.0)
    return img
}

/**
 * Apple one, star two, ball three: the star in front centre, the apple and
 * ball behind at either side, chips on last so every numeral stays legible.
 */
fun paintScene(img: Img, cx: Double, cy: Double, box: Double, chips: Boolean = true) {
    data class Seat(
        val kind: String,
        val dx: Double,
        val dy: Double,
        val chip: String,
        val ox: Double,
        val oy: Double,
    )
    // An upward arc, read left to right: the chips count 1, 2, 3 in the
    // order the eye already travels. The star sits in front, mid-arc.
    val seats = listOf(
        Seat("apple", -0.62, 0.12, "1", 0.46, -0.46),
        Seat("ball", 0.62, 0.12, "3", 0.46, -0.46),
        Seat("star", 0.0, -0.10, "2", 0.44, -0.34),
    )
    for (s in seats) {
        flatCountable(img, s.kind, cx + s.dx * box, cy + s.dy * box, box)
    }
    if (chips) {
        for (s in seats) {
            val x = cx + s.dx * box
            val y = cy + s.dy * box
            flatChip(img, x + box * s.ox, y + box * s.oy, box * 0.42, s.chip)
        }
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
    paintScene(img, px / 2.0, px * 0.52, px * 0.38)
    roundCorners(img, 0.21)
    // No unsharp here: on the low-contrast cream its edge halo shows, and a
    // straight bicubic downscale of flat candy is already crisp.
    savePng(resizeBicubic(img, 512, 512), File(root, "play-store/play-icon-512.png"))
}

private val DENSITIES = listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

fun launcherIcons(root: File) {
    for ((name, dp) in DENSITIES) {
        val legacy = iconBg(dp)
        paintScene(legacy, dp * 0.5, dp * 0.5, dp * 0.40, chips = dp >= 96)
        roundCorners(legacy, 0.22)
        savePng(legacy, File(root, "app/src/main/res/mipmap-$name/ic_launcher.png"))

        // Adaptive foreground: full-bleed cream; the trio stays inside the
        // 66/108 safe circle so any mask crops only ground.
        val canvas = (dp * 108 / 48)
        val fg = iconBg(canvas)
        paintScene(fg, canvas / 2.0, canvas / 2.0, canvas * 0.21, chips = false)
        savePng(fg, File(root, "app/src/main/res/mipmap-$name/ic_launcher_foreground.png"))
    }
}
