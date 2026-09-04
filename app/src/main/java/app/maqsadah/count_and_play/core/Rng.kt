package app.maqsadah.count_and_play.core

/**
 * Deterministic randomness, injected everywhere.
 *
 * Nothing in [core] calls `Random.nextInt()` directly, so any round the app can
 * produce can be reproduced in a unit test from a seed. This is what makes the
 * game rules provable without an emulator.
 */
interface Rng {
    /** Uniform in `[0, untilExclusive)`. */
    fun int(untilExclusive: Int): Int
}

/** Uniform in `[from, to]`, both inclusive. */
fun Rng.range(from: Int, to: Int): Int {
    require(to >= from) { "empty range $from..$to" }
    return from + int(to - from + 1)
}

fun <T> Rng.pick(items: List<T>): T {
    require(items.isNotEmpty()) { "cannot pick from an empty list" }
    return items[int(items.size)]
}

/** Fisher-Yates, driven entirely by [int] so it stays reproducible. */
fun <T> Rng.shuffled(items: List<T>): List<T> {
    val out = items.toMutableList()
    for (i in out.lastIndex downTo 1) {
        val j = int(i + 1)
        val tmp = out[i]
        out[i] = out[j]
        out[j] = tmp
    }
    return out
}

class SeededRng(seed: Long) : Rng {
    private val random = kotlin.random.Random(seed)
    override fun int(untilExclusive: Int): Int {
        require(untilExclusive > 0) { "empty bound $untilExclusive" }
        return random.nextInt(untilExclusive)
    }
}
