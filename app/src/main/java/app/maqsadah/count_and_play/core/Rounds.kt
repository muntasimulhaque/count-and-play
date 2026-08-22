package app.maqsadah.count_and_play.core

import kotlinx.collections.immutable.toPersistentList

/**
 * Round factories. All randomness flows through the injected [Rng], so every
 * round is reproducible from a seed. Levels are clamped to 0..2 defensively:
 * a stale persisted level can never crash the factories.
 */

private fun Int.asLevel(): Int = coerceIn(0, Adapt.MAX_LEVEL)

/** COUNT tray size per level. */
private fun countN(level: Int): IntRange = when (level.asLevel()) {
    0 -> 1..3
    1 -> 1..5
    else -> 1..10
}

/** ADD combined total per level; the plates split it, both >= 1. */
private fun addTotal(level: Int): IntRange = when (level.asLevel()) {
    0 -> 2..3
    1 -> 2..5
    else -> 2..10
}

/** TAKE bowl size per level. */
private fun takeN(level: Int): IntRange = when (level.asLevel()) {
    0 -> 2..3
    1 -> 3..5
    else -> 4..10
}

/** TAKE how many to remove, per level; always < n (see [TakeRound.next]). */
private fun takeB(level: Int): IntRange = when (level.asLevel()) {
    0 -> 1..1
    1 -> 1..2
    else -> 1..3
}

object CountRound {
    fun next(level: Int, rng: Rng): CountState {
        val bounds = countN(level)
        val n = rng.range(bounds.first, bounds.last)
        return CountState(tokens(n, rng))
    }
}

object AddRound {
    // No plate ever exceeds five: the trays are five-frames, and the split
    // must stay within what a phone can draw at a touch size a three-year-old
    // can actually hit (see ui/TrayMath.kt). A 9 + 1 deal also teaches less
    // than 5 + 5: the parts should each be countable at a glance.
    private const val MAX_PLATE = 5

    fun next(level: Int, rng: Rng): AddState {
        val bounds = addTotal(level)
        val total = rng.range(bounds.first, bounds.last)
        val lo = maxOf(1, total - MAX_PLATE)
        val hi = minOf(MAX_PLATE, total - 1)
        require(lo <= hi) { "split bounds collapsed at total $total" }
        val a = rng.range(lo, hi)
        val b = total - a
        val all = tokens(total, rng)
        return AddState(a, b, plateA = all.take(a).toPersistentList(), plateB = all.drop(a).toPersistentList())
    }
}

object TakeRound {
    fun next(level: Int, rng: Rng): TakeState {
        // The bounds tables must always leave at least one leftover at the
        // smallest n, or the b range below collapses and every TAKE deal
        // crashes. Pinned here so a future table edit fails loudly.
        require(takeN(level).first > takeB(level).first) {
            "takeB minimum must stay below takeN minimum at level $level"
        }
        val nBounds = takeN(level)
        val n = rng.range(nBounds.first, nBounds.last)
        val bBounds = takeB(level)
        // b < n is guaranteed by the bounds tables, but never trust the table.
        val b = rng.range(bBounds.first, minOf(bBounds.last, n - 1))
        return TakeState(n, b, tokens(n, rng))
    }
}

private fun tokens(count: Int, rng: Rng) =
    (1..count).map { Token(it, rng.pick(ShapeKind.all)) }.toPersistentList()
