package app.maqsadah.count_and_play.core

/**
 * One countable object on screen. [counted] marks a finished count (COUNT's
 * tap, ADD's plate and bowl counts, TAKE's leftover count); [gone] marks a
 * TAKE removal. [countOrder] is WHEN: the 1-based order in which this token
 * was counted or taken within the current phase, so the chip can follow the
 * child's finger rather than the tray's order (0 = not yet).
 */
data class Token(
    val id: Int,
    val shape: ShapeKind,
    val counted: Boolean = false,
    val gone: Boolean = false,
    val countOrder: Int = 0,
    /** ADD only: 1 = came from plate A, 2 = plate B, so the bowl can seat each part on its own colour. */
    val origin: Int = 0,
)

/**
 * COUNT: a tray of n tokens. The child taps each token once, in whatever
 * order he likes; each valid tap says the next counting word and the token
 * keeps a chip with that number — his tap order, not the tray's. A tap on an
 * already-counted token is recorded as a struggle but changes nothing and
 * makes no noise.
 */
data class CountState(
    val tokens: List<Token>,
    val invalidTaps: Int = 0,
) {
    val n: Int get() = tokens.size
    val done: Boolean get() = tokens.all { it.counted }

    fun onTap(id: Int): Pair<CountState, List<Beat>> {
        val token = tokens.find { it.id == id }
        if (token == null || token.counted) return struggle()
        val k = tokens.count { it.counted } + 1
        val updated = tokens.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (k == n) {
                add(Beat.SayCardinal(n))
                add(Beat.FlashCount(n))
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return CountState(updated, invalidTaps) to beats
    }

    private fun struggle(): Pair<CountState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}

/**
 * ADD, in three phases the child drives himself:
 * 1. COUNT THE PARTS: each plate is counted on its own, one counting word per
 *    tap, chips in his tap order; finishing a plate says its cardinal. The
 *    plates may be counted in any order, even interleaved — each keeps its
 *    own count.
 * 2. POUR: once both plates are counted the button wakes; tapping it pours
 *    everyone into the bowl, each part keeping its plate's colour.
 * 3. COUNT THE WHOLE: the bowl is counted afresh, one word per tap, and the
 *    last tap lands the fact: a and b make total.
 * Re-taps are recorded as struggles and change nothing.
 */
data class AddState(
    val a: Int,
    val b: Int,
    val plateA: List<Token>,
    val plateB: List<Token>,
    val bowl: List<Token> = emptyList(),
    val poured: Boolean = false,
    val invalidTaps: Int = 0,
) {
    val total: Int get() = a + b
    val countedA: Int get() = plateA.count { it.counted }
    val countedB: Int get() = plateB.count { it.counted }

    /** Both parts counted: the pour button may wake. */
    val platesReady: Boolean get() = countedA == a && countedB == b
    val done: Boolean get() = poured && bowl.all { it.counted }

    fun onTap(id: Int): Pair<AddState, List<Beat>> =
        if (poured) tapBowl(id) else tapPlate(id)

    /** The one in-round action that is not a token tap: pour the counted plates. */
    fun onPour(): Pair<AddState, List<Beat>> {
        if (poured || !platesReady) return this to emptyList()
        // Everyone is counted afresh in the bowl; only the part colour stays.
        val moved = plateA.map { it.copy(counted = false, countOrder = 0, origin = 1) } +
            plateB.map { it.copy(counted = false, countOrder = 0, origin = 2) }
        val beats = listOf(Beat.Play(Sfx.RUSTLE), Beat.SayPromptAll)
        return copy(plateA = emptyList(), plateB = emptyList(), bowl = moved, poured = true) to beats
    }

    private fun tapPlate(id: Int): Pair<AddState, List<Beat>> {
        val inA = plateA.any { it.id == id }
        val token = plateA.find { it.id == id } ?: plateB.find { it.id == id } ?: return struggle()
        if (token.counted) return struggle()
        val k = (if (inA) countedA else countedB) + 1
        val next = copy(
            plateA = plateA.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it },
            plateB = plateB.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it },
        )
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (k == (if (inA) a else b)) add(Beat.SayCardinal(k))
            if (next.platesReady) add(Beat.SayPromptAdd)
        }
        return next to beats
    }

    private fun tapBowl(id: Int): Pair<AddState, List<Beat>> {
        val token = bowl.find { it.id == id } ?: return struggle()
        if (token.counted) return struggle()
        val k = bowl.count { it.counted } + 1
        val next = copy(
            bowl = bowl.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it },
        )
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (next.done) {
                add(Beat.SayFactAdd(a, b, total))
                add(Beat.FlashAdd(a, b, total))
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return next to beats
    }

    private fun struggle(): Pair<AddState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}

/**
 * TAKE, in two phases the child drives himself:
 * 1. TAKE AWAY: each tap removes one token — THUD, its take-away number, and
 *    the token sinks into its ghost wearing that number. The b-th removal
 *    asks how many are left.
 * 2. COUNT THE LEFT: the child counts the leftovers himself, one word per
 *    tap, chips in his tap order; the last one lands the fact: n take away b
 *    leaves left.
 * Re-taps and ghost taps are recorded as struggles and change nothing.
 */
data class TakeState(
    val n: Int,
    val b: Int,
    val tokens: List<Token>,
    val invalidTaps: Int = 0,
) {
    val removed: Int get() = tokens.count { it.gone }
    val left: Int get() = n - removed

    /** The asked number is out: time to count what is left. */
    val removalDone: Boolean get() = removed == b
    val done: Boolean get() = removalDone && tokens.all { it.gone || it.counted }

    fun onTap(id: Int): Pair<TakeState, List<Beat>> {
        val token = tokens.find { it.id == id }
        if (token == null || token.gone || token.counted || done) return struggle()
        return if (removalDone) countLeft(id) else remove(id)
    }

    private fun remove(id: Int): Pair<TakeState, List<Beat>> {
        val k = removed + 1
        val updated = tokens.map { if (it.id == id) it.copy(gone = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.THUD))
            add(Beat.SayCount(k))
            if (k == b) add(Beat.SayPromptLeft)
        }
        return TakeState(n, b, updated, invalidTaps) to beats
    }

    private fun countLeft(id: Int): Pair<TakeState, List<Beat>> {
        val k = tokens.count { !it.gone && it.counted } + 1
        val updated = tokens.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (k == left) {
                add(Beat.SayFactTake(n, b, left))
                add(Beat.FlashTake(n, b, left))
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return TakeState(n, b, updated, invalidTaps) to beats
    }

    private fun struggle(): Pair<TakeState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}
