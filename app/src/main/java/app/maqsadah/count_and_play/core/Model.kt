package app.maqsadah.count_and_play.core

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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
 * keeps a chip with that number: his tap order, not the tray's. A tap on an
 * already-counted token is recorded as a struggle but changes nothing and
 * makes no noise.
 */
data class CountState(
    val tokens: PersistentList<Token>,
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
            // The total lands with the last tap itself, never a few words
            // later: he sees the sum the instant he finishes counting.
            if (k == n) add(Beat.FlashCount(n))
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            // A single-object tray would say its number twice back to back;
            // the counting word already IS the cardinal there.
            if (k == n && n > 1) {
                add(Beat.SayCardinal(n))
            }
            if (k == n) {
                // The finished count earns its own plain word of praise,
                // spoken over the card.
                add(Beat.SayPraise)
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return CountState(updated.toPersistentList(), invalidTaps) to beats
    }

    private fun struggle(): Pair<CountState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}

/**
 * ADD, in three phases the child drives himself:
 * 1. COUNT THE PARTS: each plate is counted on its own, one counting word per
 *    tap, chips in his tap order; finishing a plate says its cardinal and pops
 *    the plate's total onto the plate itself. The LEFT plate must be counted
 *    out first; the right plate sleeps (shown washed-out) until then, so two
 *    columns can never be mixed into one count.
 * 2. POUR: once both plates are counted the button wakes; tapping it pours
 *    everyone into the bowl, each part keeping its plate's colour. The plates
 *    stay exactly as they were, now wearing their totals; only the objects
 *    move down into the bowl that appears beneath them.
 * 3. COUNT THE WHOLE: the bowl is counted afresh, one word per tap, and the
 *    last tap lands the card while the voice says the fact: a and b make total.
 * Re-taps are recorded as struggles and change nothing.
 */
data class AddState(
    val a: Int,
    val b: Int,
    val plateA: PersistentList<Token>,
    val plateB: PersistentList<Token>,
    val bowl: PersistentList<Token> = persistentListOf(),
    val poured: Boolean = false,
    /** Plate A fully counted: its total may be worn, and plate B wakes. Survives the pour. */
    val doneA: Boolean = false,
    /** Plate B fully counted: its total may be worn. Survives the pour. */
    val doneB: Boolean = false,
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

    /**
     * The one in-round action that is not a token tap: pour the counted plates.
     * Pouring before both plates are counted plays a soft tick so the touch is
     * never dead, records no struggle (the asleep button is the UI's job to
     * prevent), and changes nothing.
     */
    fun onPour(): Pair<AddState, List<Beat>> {
        if (poured || !platesReady) return this to listOf(Beat.Play(Sfx.TICK))
        // Everyone is counted afresh in the bowl; only the part colour stays.
        val moved = plateA.map { it.copy(counted = false, countOrder = 0, origin = 1) } +
            plateB.map { it.copy(counted = false, countOrder = 0, origin = 2) }
        val beats = listOf(Beat.Play(Sfx.RUSTLE), Beat.SayPromptAll)
        return copy(
            plateA = persistentListOf(),
            plateB = persistentListOf(),
            bowl = moved.toPersistentList(),
            poured = true,
        ) to beats
    }

    private fun tapPlate(id: Int): Pair<AddState, List<Beat>> {
        val inA = plateA.any { it.id == id }
        val token = plateA.find { it.id == id } ?: plateB.find { it.id == id } ?: return struggle()
        if (token.counted) return struggle()
        // The right plate is asleep until the left one has been counted out;
        // a tap there answers with the soft tick and records the reach, but
        // never counts.
        if (!inA && !doneA) return asleep()
        val plateSize = if (inA) a else b
        val k = (if (inA) countedA else countedB) + 1
        val next = copy(
            plateA = plateA.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }.toPersistentList(),
            plateB = plateB.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }.toPersistentList(),
            doneA = doneA || (inA && k == plateSize),
            doneB = doneB || (!inA && k == plateSize),
        )
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            // A one-token plate just said "one" as its count word; repeating
            // it as the cardinal stutters at the most formative rounds.
            if (k == plateSize && k > 1) add(Beat.SayCardinal(k))
            if (next.platesReady) add(Beat.SayPromptAdd)
        }
        return next to beats
    }

    /** A touch on the sleeping plate: heard softly, remembered, not counted. */
    private fun asleep(): Pair<AddState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to listOf(Beat.Play(Sfx.TICK))

    private fun tapBowl(id: Int): Pair<AddState, List<Beat>> {
        val token = bowl.find { it.id == id } ?: return struggle()
        if (token.counted) return struggle()
        val k = bowl.count { it.counted } + 1
        val next = copy(
            bowl = bowl.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }.toPersistentList(),
        )
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (next.done) {
                // The card lands as the fact begins, never after it is spoken.
                add(Beat.FlashAdd(a, b, total))
                add(Beat.SayFactAdd(a, b, total))
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
 * TAKE, in three phases the child drives himself:
 * 1. COUNT THE WHOLE: the tray is counted first, one word per tap, chips in
 *    his tap order; the last tap names the total and asks the subtraction.
 *    The ask hangs as numerals (n − b) only from here on: symbols arrive
 *    with the act they name, never earlier.
 * 2. TAKE AWAY: each tap removes one token: THUD, its take-away number, and
 *    the token sinks into its ghost wearing that number. The b-th removal
 *    asks how many are left.
 * 3. COUNT THE LEFT: the child counts the leftovers himself, one word per
 *    tap, chips in his tap order; the last one lands the fact: n take away b
 *    leaves left.
 * Re-taps and ghost taps are recorded as struggles and change nothing.
 */
data class TakeState(
    val n: Int,
    val b: Int,
    val tokens: PersistentList<Token>,
    /** The whole tray has been counted and the take-away ask is up. */
    val totalDone: Boolean = false,
    val invalidTaps: Int = 0,
) {
    val removed: Int get() = tokens.count { it.gone }
    val left: Int get() = n - removed

    /** The asked number is out: time to count what is left. */
    val removalDone: Boolean get() = removed == b
    val done: Boolean get() = totalDone && removalDone && tokens.all { it.gone || it.counted }

    fun onTap(id: Int): Pair<TakeState, List<Beat>> {
        val token = tokens.find { it.id == id }
        if (token == null || token.gone || token.counted || done) return struggle()
        return when {
            !totalDone -> countTotal(id)
            !removalDone -> remove(id)
            else -> countLeft(id)
        }
    }

    private fun countTotal(id: Int): Pair<TakeState, List<Beat>> {
        val k = tokens.count { it.counted } + 1
        val counted = tokens.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            // The count word already IS the cardinal on a one-token tray; a
            // tray that small never deals here, but the guard costs nothing.
            if (k == n && n > 1) add(Beat.SayCardinal(n))
            if (k == n) add(Beat.SayPromptTake(b))
        }
        // The whole is known: the chips hand their numbers back, so the take
        // and the left count can each wear their own, exactly as the ADD bowl
        // resets when everyone pours.
        val updated = if (k == n) {
            counted.map { it.copy(counted = false, countOrder = 0) }
        } else {
            counted
        }
        return copy(tokens = updated.toPersistentList(), totalDone = totalDone || k == n) to beats
    }

    private fun remove(id: Int): Pair<TakeState, List<Beat>> {
        val k = removed + 1
        val updated = tokens.map { if (it.id == id) it.copy(gone = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.THUD))
            add(Beat.SayCount(k))
            if (k == b) add(Beat.SayPromptLeft)
        }
        return copy(tokens = updated.toPersistentList()) to beats
    }

    private fun countLeft(id: Int): Pair<TakeState, List<Beat>> {
        val k = tokens.count { !it.gone && it.counted } + 1
        val updated = tokens.map { if (it.id == id) it.copy(counted = true, countOrder = k) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.TICK))
            add(Beat.SayCount(k))
            if (k == left) {
                // The card lands as the fact begins, never after it is spoken.
                add(Beat.FlashTake(n, b, left))
                add(Beat.SayFactTake(n, b, left))
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return copy(tokens = updated.toPersistentList()) to beats
    }

    private fun struggle(): Pair<TakeState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}
