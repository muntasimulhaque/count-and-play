package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val plateSol = solveTray(
        plateWidth, bigPlate, AddCap, room - BowlAsleepReserve, minNode = PlateHitTarget,
    )
    val bowlSeed = solveTray(playWidth, total, AddCap, room - BowlAsleepReserve - SectionGap * 2, seated = true)
    var scale = 1f
    while (scale > 0.4f) {
        val plate = plateSol.size * scale
        val bowl = bowlSeed.size * scale
        val bowlFull = trayHeight(total, bowl, bowlSeed.perRow, seated = true)
        if (trayHeight(bigPlate, plate, plateSol.perRow, minNode = PlateHitTarget) +
            SectionGap * 2 + bowlFull <= room
        ) {
            return TraySizes(plate, plate, bowl, plateSol.perRow, bowlSeed.perRow, bowlFull, true)
        }
        scale -= 0.05f
    }
    val bowlSol = solveTray(playWidth, total, AddCap, room - PouredPlatePlace - TrayPad * 2 - SectionGap * 2, seated = true)
    return TraySizes(plateSol.size, PouredPlatePlace, bowlSol.size, plateSol.perRow, bowlSol.perRow, BowlAsleepReserve, false)
}
