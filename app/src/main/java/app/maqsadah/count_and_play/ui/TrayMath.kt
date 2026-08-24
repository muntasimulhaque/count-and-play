package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tray geometry, as pure arithmetic so it can be unit-tested without a device.
 *
 * ADD must fit two trays and the pour button into whatever height a phone
 * offers, in BOTH phases: plates full with an empty bowl waiting, and the
 * poured bowl full with the emptied plates keeping their place. The solvers
 * below size every tray for its round's numbers alone, so nothing jumps when
 * the plates pour.
 *
 * Two facts of the real layout are priced in here rather than ignored:
 *
 * - Every token node is at least [HitTarget] wide, whatever it looks like;
 *   a three-year-old's finger lands wide, and packing follows the finger,
 *   not the drawing.
 * - A seated token (the bowl's part colours) draws a circle around itself,
 *   so its node grows past its body.
 *
 * Rows follow [perRowTemplate]: balanced arrangements with no lonely orphan
 * row, so a tray of four is a line of four or a square, never three-and-one.
 */

internal val TrayPad = 14.dp
internal val TrayGap = 10.dp

/** A three-year-old's finger lands wide: the smallest tappable node. */
internal val HitTarget = 72.dp

/** How much a seated token's node exceeds its body. */
internal const val SeatScale = 1.3f

/** Object-size ceilings: one-tray games may grow huge, ADD shares a screen. */
internal val SingleCap = 128.dp
internal val AddCap = 96.dp

/** The floor below which shrinking stops entirely. */
internal val MinObject = 24.dp

// -- Shared layout measures for the ADD column -------------------------------

/** Vertical room the pour button reserves, invisible spacer included. */
internal val PourReserve = 76.dp
internal val SectionGap = 14.dp
internal val PlateGap = 14.dp

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
}

/** One solved tray: how big each object draws, and how many sit in a row. */
internal data class TraySolution(val size: Dp, val perRow: Int)

/** The node a token of [size] occupies: touch floor, plus seat growth. */
internal fun nodeOf(size: Dp, seated: Boolean): Dp =
    maxOf(HitTarget, if (seated) size * SeatScale else size)

internal fun rowsFor(count: Int, perRow: Int): Int =
    if (count <= 0) 0 else (count + perRow - 1) / perRow

/** Full rendered height of a tray, rim and padding included. */
internal fun trayHeight(count: Int, size: Dp, perRow: Int, seated: Boolean = false): Dp {
    val rows = rowsFor(count, perRow)
    if (rows <= 0) return size + TrayPad * 2
    val node = nodeOf(size, seated)
    return node * rows + TrayGap * (rows - 1) + TrayPad * 2
}

/** The widest row of touch-sized nodes that fits a tray's inner width. */
private fun maxPerRowFor(inner: Dp): Int =
    ((inner + TrayGap) / (HitTarget + TrayGap)).toInt().coerceAtLeast(1)

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
): TraySolution {
    if (count <= 0) return TraySolution(cap, 1)
    val inner = width - TrayPad * 2
    val pref = minOf(perRowTemplate(count), maxPerRowFor(inner))
    for (perRow in pref downTo 1) {
        var size = minOf(cap, (inner - TrayGap * (perRow - 1)) / perRow / if (seated) SeatScale else 1f)
        if (size < MinObject) continue
        var node = nodeOf(size, seated)
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

/** One solved ADD round: plate and bowl sizes plus their row counts. */
internal data class TraySizes(
    val plate: Dp,
    val bowl: Dp,
    val platePerRow: Int,
    val bowlPerRow: Int,
)

/**
 * Sizes both ADD trays for a round of [bigPlate]-and-the-rest totalling
 * [total], inside [availHeight]. Both phases must fit: the plates counted
 * with an empty bowl waiting, and the poured bowl counted with the emptied
 * plates keeping their place. Each tray first gets its biggest solo size,
 * then both shrink together only as much as the shared height demands.
 */
internal fun solveAddTraySizes(
    playWidth: Dp,
    bigPlate: Int,
    total: Int,
    availHeight: Dp,
): TraySizes {
    val room = availHeight - PourReserve - SectionGap * 2
    val plateWidth = (playWidth - PlateGap) / 2
    val plateSol = solveTray(plateWidth, bigPlate, AddCap, room)
    val bowlSol = solveTray(playWidth, total, AddCap, room, seated = true)
    var scale = 1f
    while (scale > 0.4f) {
        val plate = plateSol.size * scale
        val bowl = bowlSol.size * scale
        val beforePour =
            trayHeight(bigPlate, plate, plateSol.perRow) + bowl + TrayPad * 2 <= room
        val afterPour =
            plate + TrayPad * 2 + trayHeight(total, bowl, bowlSol.perRow, seated = true) <= room
        if (beforePour && afterPour) {
            return TraySizes(plate, bowl, plateSol.perRow, bowlSol.perRow)
        }
        scale -= 0.05f
    }
    // Nothing fit: keep both trays presentable rather than optimal.
    return TraySizes(MinObject, MinObject, 1, 1)
}
