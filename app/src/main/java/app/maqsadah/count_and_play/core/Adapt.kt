package app.maqsadah.count_and_play.core

/**
 * Invisible difficulty for one skill. Two clean rounds (zero invalid taps) in
 * a row level up; a round with 3+ invalid taps levels down; a single slip
 * keeps the streak (motor noise is not confusion), two slips reset it. The
 * child never sees any of this.
 */
data class Adapt(val level: Int = 0, val streak: Int = 0) {

    fun record(invalidTaps: Int): Adapt = when {
        invalidTaps == 0 -> {
            val streak = streak + 1
            if (streak >= 2) Adapt((level + 1).coerceAtMost(MAX_LEVEL), 0)
            else Adapt(level, streak)
        }
        // One slip keeps the streak: an accidental double-tap says nothing
        // about whether the mathematics landed.
        invalidTaps == 1 -> this
        invalidTaps == 2 -> Adapt(level, 0)
        else -> Adapt((level - 1).coerceAtLeast(0), 0)
    }

    companion object {
        const val MAX_LEVEL = 2
    }
}
