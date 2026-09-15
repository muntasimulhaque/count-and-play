package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp

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
