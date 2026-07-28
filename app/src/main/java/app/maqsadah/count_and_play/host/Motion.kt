package app.maqsadah.count_and_play.host

import app.maqsadah.count_and_play.core.StageChange

/**
 * How long every visible change takes.
 *
 * These constants are shared by the animation and by the script runner that
 * waits for it, so narration can never arrive before the objects have landed —
 * on a fast phone or a slow one. Timings are set for a 3-year-old's tracking,
 * which is markedly slower than an adult's: the old build dropped objects every
 * 110 ms and teleported them on merge.
 */
object Motion {
    const val DROP = 300L
    const val DROP_STAGGER = 150L
    const val TRAVEL = 520L
    const val TRAVEL_STAGGER = 120L
    const val COLLAPSE = 560L
    const val POP = 260L
    const val COMPACT = 340L
    const val COVER = 420L
    const val PREDICTION_IN = 320L
    const val CELEBRATE = 900L

    /** A pulse runs alongside play rather than blocking it. */
    const val BREATHE = 900L

    fun durationOf(change: StageChange): Long = when (change) {
        is StageChange.Drop ->
            DROP + DROP_STAGGER * (change.ids.size - 1).coerceAtLeast(0)
        is StageChange.Travel ->
            TRAVEL + TRAVEL_STAGGER * (change.ids.size - 1).coerceAtLeast(0)
        is StageChange.Collapse -> COLLAPSE
        is StageChange.Compact -> COMPACT
        is StageChange.Cover, is StageChange.Uncover -> COVER
        StageChange.ShowPrediction -> PREDICTION_IN
        // The confetti runs alongside the words. A celebration the game has to
        // stop and wait for is not a celebration, it is another pause.
        StageChange.Celebrate -> 0L
        // Silent, spatial, and non-blocking: it says "here" without stopping
        // anything or demanding a response.
        is StageChange.Highlight -> 0L
    }
}
