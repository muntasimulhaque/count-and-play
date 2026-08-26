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

// -- Shared layout measures for the game columns ------------------------------

/** Vertical room the sleeping bowl strip reserves before the pour. */
internal val BowlAsleepReserve = 76.dp
internal val SectionGap = 14.dp
internal val PlateGap = 14.dp

/** Vertical room the take-away equation reserves above the trays. */
internal val TakeEqReserve = 56.dp

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
internal fun maxPerRowFor(inner: Dp): Int =
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

/** One solved ADD round: plate and bowl sizes plus their row counts.
 *
 *  [plate] sizes phase one. [plateAfter] sizes the poured plates' places:
 *  usually equal to [plate] (the columns stand unchanged beneath the bowl);
 *  on screens too squat to hold both full columns and the bowl, it is the
 *  slim strip height the folded plates keep, wearing their totals.
 *
 *  [bowlBefore] is the height the bowl reserves in phase one, while it is
 *  still asleep. When the screen is roomy it is the bowl's full seated
 *  height and [bowlInPlace] is true: the empty seats he sees while counting
 *  the plates are exactly the seats the pieces land in on the pour, so
 *  nothing on screen moves but the pieces. On tight screens it is a slim
 *  strip (the bowl-as-destination still present, just folded), and the bowl
 *  arrives full-size only with the pour.
 */
internal data class TraySizes(
    val plate: Dp,
    val plateAfter: Dp,
    val bowl: Dp,
    val platePerRow: Int,
    val bowlPerRow: Int,
    val bowlBefore: Dp,
    val bowlInPlace: Boolean,
)

/** The object size that defines a folded plate's slim post-pour place. */
internal val PouredPlatePlace = 56.dp

/**
 * Sizes one ADD round. The bowl is the pour's destination, so it is on
 * screen from the first frame: asleep beneath the plates, waking when both
 * are counted. The solver's first choice is therefore also the quietest
 * layout: plates and the full-size bowl fit together from the start, the
 * plates never resize, and the pour moves pieces, not furniture. Only when
 * the screen is too squat for that do the sleeping bowl fold into a slim
 * strip for phase one, the poured plates fold into slim places wearing
 * their totals, and the bowl take the freed height.
 */
internal fun solveAddTraySizes(
    playWidth: Dp,
    bigPlate: Int,
    total: Int,
    availHeight: Dp,
): TraySizes {
    val room = availHeight
    val plateWidth = (playWidth - PlateGap) / 2
    val plateSol = solveTray(plateWidth, bigPlate, AddCap, room - BowlAsleepReserve)
    val bowlSeed = solveTray(playWidth, total, AddCap, room - BowlAsleepReserve - SectionGap * 2, seated = true)
    var scale = 1f
    while (scale > 0.4f) {
        val plate = plateSol.size * scale
        val bowl = bowlSeed.size * scale
        val bowlFull = trayHeight(total, bowl, bowlSeed.perRow, seated = true)
        if (trayHeight(bigPlate, plate, plateSol.perRow) + SectionGap * 2 + bowlFull <= room) {
            return TraySizes(plate, plate, bowl, plateSol.perRow, bowlSeed.perRow, bowlFull, true)
        }
        scale -= 0.05f
    }
    val bowlSol = solveTray(playWidth, total, AddCap, room - PouredPlatePlace - TrayPad * 2 - SectionGap * 2, seated = true)
    return TraySizes(plateSol.size, PouredPlatePlace, bowlSol.size, plateSol.perRow, bowlSol.perRow, BowlAsleepReserve, false)
}

/** One solved TAKE round: one object size shared by both trays, so a token keeps its figure when it moves down. */
internal data class TakeSolution(
    val size: Dp,
    val mainPerRow: Int,
    val takenPerRow: Int,
)

/**
 * Sizes a TAKE round: the main tray above, the equation between prompt and
 * play, and the taken-away box below. Both trays share one object size, so
 * a token that moves down never changes shape mid-flight.
 */
internal fun solveTakeSizes(
    playWidth: Dp,
    n: Int,
    gone: Int,
    availHeight: Dp,
): TakeSolution {
    val gt = maxOf(gone, 1) // an empty taken box still claims one row of place
    val takenPerRow = perRowTemplate(gt).coerceAtMost(maxPerRowFor(playWidth - TrayPad * 2))
    val room = availHeight - TakeEqReserve - SectionGap * 2
    val mainSol = solveTray(playWidth, n, SingleCap, room - trayHeight(gt, MinObject, takenPerRow))
    var scale = 1f
    while (scale > 0.4f) {
        val s = mainSol.size * scale
        val need = trayHeight(n, s, mainSol.perRow) + trayHeight(gt, s, takenPerRow)
        if (need <= room) return TakeSolution(s, mainSol.perRow, takenPerRow)
        scale -= 0.05f
    }
    return TakeSolution(MinObject, 1, 1)
}
