package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tray geometry, as pure arithmetic so it can be unit-tested without a device.
 *
 * ADD must fit two trays and the pour button into whatever height a phone
 * offers, in BOTH phases: plates full with an empty bowl, and the poured bowl
 * full with empty plates. [solveAddTraySizes] sizes both trays together so
 * neither phase ever overflows, on anything from a narrow phone to a tablet.
 *
 * Two facts of the real layout are priced in here rather than ignored:
 *
 * - Every token node is at least [HitTarget] wide, whatever it looks like;
 *   a three-year-old's finger lands wide, and packing follows the finger,
 *   not the drawing.
 * - A seated token (the bowl's part colours) draws a circle around itself,
 *   so its node grows past its body.
 */

internal val TrayPad = 14.dp
internal val TrayGap = 10.dp

/** A three-year-old's finger lands wide: the smallest tappable node. */
internal val HitTarget = 72.dp

/** How much a seated token's node exceeds its body. */
internal const val SeatScale = 1.3f

/** Object-size ceilings by tray crowding: few tokens stay huge, many shrink. */
internal fun objectCap(count: Int): Dp = if (count <= 5) 96.dp else 64.dp

/** The floor below which shrinking stops entirely. */
internal val MinObject = 24.dp

// -- Shared layout measures for the ADD column -------------------------------

/** Vertical room the pour button reserves, invisible spacer included. */
internal val PourReserve = 76.dp
internal val SectionGap = 14.dp
internal val PlateGap = 14.dp
internal const val MaxPlateRows = 4
internal const val MaxBowlRows = 4

/** The node a token of [size] occupies: touch floor, plus seat growth. */
internal fun nodeOf(size: Dp, seated: Boolean): Dp =
    maxOf(HitTarget, if (seated) size * SeatScale else size)

/** How many objects of [size] pack into one row of a tray [width] wide. */
private fun perRowFor(width: Dp, count: Int, size: Dp, seated: Boolean): Int {
    val node = nodeOf(size, seated)
    val inner = width - TrayPad * 2
    return (((inner + TrayGap) / (node + TrayGap)).toInt()).coerceIn(1, maxOf(1, count))
}

/** Rows [count] objects of [size] fill in a tray [width] wide. */
internal fun trayRows(width: Dp, count: Int, size: Dp, seated: Boolean = false): Int {
    if (count <= 0) return 0
    val perRow = perRowFor(width, count, size, seated)
    return (count + perRow - 1) / perRow
}

/** Full rendered height of that tray, rim and padding included. */
internal fun trayHeight(width: Dp, count: Int, size: Dp, seated: Boolean = false): Dp {
    val rows = trayRows(width, count, size, seated)
    if (rows <= 0) return size + TrayPad * 2
    val node = nodeOf(size, seated)
    return node * rows + TrayGap * (rows - 1) + TrayPad * 2
}

/**
 * The largest object size at most [cap] that keeps [count] objects within
 * [maxRows] rows and the whole tray inside [budget] height. Every row count
 * is tried and the biggest feasible object wins, so a few objects stay huge
 * and only genuinely crowded trays shrink.
 */
internal fun fitTraySize(
    width: Dp,
    count: Int,
    cap: Dp,
    maxRows: Int,
    budget: Dp,
    seated: Boolean = false,
): Dp {
    if (count <= 0) return cap
    var best = MinObject
    for (rows in 1..minOf(maxRows, count)) {
        val perRow = (count + rows - 1) / rows
        val size = minOf(cap, (width - TrayPad * 2 - TrayGap * (perRow - 1)) / perRow)
        if (size > best && trayHeight(width, count, size, seated) <= budget) best = size
    }
    return best
}

/** One solved round: how big a plate token and a bowl token may draw. */
internal data class TraySizes(val plate: Dp, val bowl: Dp)

/**
 * Sizes both ADD trays for a round of [bigPlate]-and-the-rest totalling
 * [total], inside [availHeight]. Both phases must fit: the plates counted
 * with an empty bowl waiting, and the poured bowl counted with the emptied
 * plates keeping their place. Prefers the largest plate size, then the
 * largest bowl; falls back to the floor sizes on a screen nothing fits.
 */
internal fun solveAddTraySizes(
    playWidth: Dp,
    bigPlate: Int,
    total: Int,
    availHeight: Dp,
): TraySizes {
    val room = availHeight - PourReserve - SectionGap * 2
    val plateWidth = (playWidth - PlateGap) / 2
    val bowlFloor = MinObject + TrayPad * 2
    var best = TraySizes(MinObject, MinObject)
    for (plateRows in MaxPlateRows downTo 1) {
        val plate = fitTraySize(plateWidth, bigPlate, objectCap(bigPlate), plateRows, room)
        for (bowlRows in MaxBowlRows downTo 1) {
            val bowl = fitTraySize(playWidth, total, objectCap(total), bowlRows, room, seated = true)
            val beforePour = trayHeight(plateWidth, bigPlate, plate) + bowl + TrayPad * 2 <= room
            val afterPour =
                plate + TrayPad * 2 + trayHeight(playWidth, total, bowl, seated = true) <= room
            val better = plate > best.plate || (plate == best.plate && bowl > best.bowl)
            if (beforePour && afterPour && better) best = TraySizes(plate, bowl)
        }
    }
    // Nothing fit: keep both trays presentable rather than optimal.
    if (best.plate == MinObject && best.bowl == MinObject) {
        return TraySizes(MinObject + TrayPad * 2, bowlFloor)
    }
    return best
}
