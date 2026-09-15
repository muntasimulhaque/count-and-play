package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tray geometry, as pure arithmetic so it can be unit-tested without a device.
 *
 * ADD must fit the plates in BOTH phases of its game: counted alone with only
 * the pour key waiting, then poured, when the bowl slides in beneath the
 * unchanged plates. TAKE must fit its tray, the equation above it and the
 * taken-away box below it. The solvers below size every tray for its round's
 * numbers alone, so nothing overlaps at any deal a child can be given.
 *
 * Two facts of the real layout are priced in here rather than ignored:
 *
 * - Every token node is at least [HitTarget] wide, whatever it looks like;
 *   a three-year-old's finger lands wide, and packing follows the finger,
 *   not the drawing.
 * - A seated token (the bowl's part colors) draws a circle around itself,
 *   so its node grows past its body.
 *
 * Rows follow [perRowTemplate]: balanced arrangements with no lonely orphan
 * row, so a tray of four is a line of four or a square, never three-and-one.
 *
 * This file owns the shared measures and the single-tray solver; the two
 * multi-tray rounds live in AddLayout.kt and TakeLayout.kt.
 */

internal val TrayPad = 14.dp
internal val TrayGap = 10.dp

/** A three-year-old's finger lands wide: the smallest tappable node. */
internal val HitTarget = 72.dp

/**
 * The half-width ADD plates need a smaller floor than a full-width tray. On a
 * 360 dp phone a plate is about 157 dp wide: at [HitTarget] two objects plus
 * their gap do not fit, so a five-object plate would stack one-wide, which is
 * not a five-frame at all. 56 dp is still a wide toddler target (well past the
 * 48 dp accessibility floor) and buys two objects per row on the smallest
 * phone this app installs on.
 */
internal val PlateHitTarget = 56.dp

/** How much a seated token's node exceeds its body. */
internal const val SeatScale = 1.3f

/**
 * Object-size ceilings. One-tray games may grow huge, ADD shares a screen so
 * its ceiling is lower, and both are sized for a toddler's eye, not a phone's
 * width: on tablets these are what bind, and the toys should grow to fill the
 * room rather than float in a sea of white liner.
 */
internal val SingleCap = 180.dp
internal val AddCap = 132.dp

/** The floor below which shrinking stops entirely. */
internal val MinObject = 24.dp

// -- Shared layout measures for the game columns ------------------------------

/** Vertical room the sleeping bowl strip reserves before the pour. */
internal val BowlAsleepReserve = 76.dp
internal val SectionGap = 14.dp
internal val PlateGap = 14.dp

/** Vertical room the take-away equation reserves above the trays. */
internal val TakeEqReserve = 56.dp

/**
 * The take equation's line box. The equation hangs above the tray, so its
 * measured height must live inside [TakeEqReserve] at every font scale the
 * app allows (see MainActivity.MAX_FONT_SCALE): 42 dp * 1.3 = 54.6 dp < 56 dp.
 * Without a tight line box Baloo's default leading made the row taller than
 * the reserve, and the taken box could run off the bottom of short screens.
 */
internal val EquationLine = 42.dp

/**
 * How many objects sit in one row of a tray of [count]: balanced
 * arrangements, never a lonely orphan row. 3 stays a line, 4 a line of four,
 * 5 the five-frame 3+2, then 3+3, 4+3, 4+4, 5+4 and 5+5.
 */
internal fun perRowTemplate(count: Int): Int = when {
    count <= 3 -> count
    count == 4 -> 4
    count <= 6 -> 3
    count <= 8 -> 4
    else -> 5
}.coerceAtLeast(1)

/** One solved tray: how big each object draws, and how many sit in a row. */
internal data class TraySolution(val size: Dp, val perRow: Int)

/**
 * How [count] items divide into rows of at most [perRow]: full rows first,
 * then the remainder centered beneath them (the tray layout places every row
 * centered, so 5 at three-per-row reads as the classic 3-over-2).
 */
internal fun rowPlan(count: Int, perRow: Int): List<Int> {
    val p = perRow.coerceAtLeast(1)
    if (count <= 0) return emptyList()
    return List(count / p) { p } + (if (count % p > 0) listOf(count % p) else emptyList())
}

/** The node a token of [size] occupies: touch floor, plus seat growth. */
internal fun nodeOf(size: Dp, seated: Boolean, minNode: Dp = HitTarget): Dp =
    maxOf(minNode, if (seated) size * SeatScale else size)

internal fun rowsFor(count: Int, perRow: Int): Int =
    if (count <= 0 || perRow <= 0) 0 else (count + perRow - 1) / perRow

/** Full rendered height of a tray, rim and padding included. */
internal fun trayHeight(
    count: Int,
    size: Dp,
    perRow: Int,
    seated: Boolean = false,
    minNode: Dp = HitTarget,
): Dp {
    val rows = rowsFor(count, perRow)
    if (rows <= 0) return size + TrayPad * 2
    val node = nodeOf(size, seated, minNode)
    return node * rows + TrayGap * (rows - 1) + TrayPad * 2
}

/** The widest row of touch-sized nodes that fits a tray's inner width. */
internal fun maxPerRowFor(inner: Dp, minNode: Dp = HitTarget): Int =
    ((inner + TrayGap) / (minNode + TrayGap)).toInt().coerceAtLeast(1)

/**
 * Solves one tray: the biggest object size at most [cap] whose template row
 * (or a narrower one, on narrow trays) fits the width at touch size and the
 * height inside [availHeight] when given. Prefers the widest balanced row,
 * so objects stay as big as the screen truly allows.
 */
internal fun solveTray(
    width: Dp,
    count: Int,
    cap: Dp,
    availHeight: Dp? = null,
    seated: Boolean = false,
    minNode: Dp = HitTarget,
): TraySolution {
    if (count <= 0) return TraySolution(cap, 1)
    val inner = width - TrayPad * 2
    val pref = minOf(perRowTemplate(count), maxPerRowFor(inner, minNode))
    for (perRow in pref downTo 1) {
        var size = minOf(cap, (inner - TrayGap * (perRow - 1)) / perRow / if (seated) SeatScale else 1f)
        if (size < MinObject) continue
        var node = nodeOf(size, seated, minNode)
        if (node * perRow + TrayGap * (perRow - 1) > inner) continue
        if (availHeight != null) {
            val rows = rowsFor(count, perRow)
            val nodeCap = (availHeight - TrayPad * 2 - TrayGap * (rows - 1)) / rows
            if (nodeCap < node) {
                node = nodeCap
                size = minOf(size, if (seated) nodeCap / SeatScale else nodeCap)
                if (size < MinObject) continue
            }
        }
        return TraySolution(size, perRow)
    }
    return TraySolution(MinObject, 1)
}

/**
 * The finished plate's total badge. Sized from the plate it belongs to, floored
 * so the numeral stays legible and capped so it reads as a tag rather than a
 * second plate. The badge rides the well's bottom-right corner, hanging just
 * past it (see PlateTray), so it never covers a piece.
 */
internal fun badgeDiameter(objectSize: Dp): Dp =
    (objectSize * 0.5f).coerceIn(34.dp, 40.dp)

/** How far the badge's corner hangs past the well's corner, both axes. */
internal val BadgeOverhang = 10.dp
