package app.maqsadah.count_and_play.core

/**
 * One countable object on screen. [counted] is used by COUNT (tapped) and ADD
 * (moved to the bowl); [gone] is used by TAKE (removed from the bowl).
 */
data class Token(
    val id: Int,
    val shape: ShapeKind,
    val counted: Boolean = false,
    val gone: Boolean = false,
    /** ADD only: 1 = came from plate A, 2 = plate B, so the bowl can seat each part on its own colour. */
    val origin: Int = 0,
)

/**
 * COUNT: a tray of n tokens. The child taps each token once; each valid tap
 * says the next counting word. A tap on an already-counted token is recorded
 * as a struggle but changes nothing and makes no noise.
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
        val updated = tokens.map { if (it.id == id) it.copy(counted = true) else it }
        val k = updated.count { it.counted }
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
 * ADD: two plates holding a and b tokens. Each valid tap moves one token to
 * the bowl; the voice counts on over the combined sequence no matter which
 * plate the token came from (one global counter over all a+b taps). Tapping
 * the bowl itself is a struggle.
 */
data class AddState(
    val a: Int,
    val b: Int,
    val plateA: List<Token>,
    val plateB: List<Token>,
    val bowl: List<Token> = emptyList(),
    val invalidTaps: Int = 0,
) {
    val total: Int get() = a + b
    val done: Boolean get() = plateA.isEmpty() && plateB.isEmpty()

    fun onTap(id: Int): Pair<AddState, List<Beat>> {
        // Not on either plate (already in the bowl, or nowhere) = struggle.
        val fromA = plateA.any { it.id == id }
        val token = plateA.find { it.id == id } ?: plateB.find { it.id == id } ?: return struggle()
        val next = copy(
            plateA = plateA.filterNot { it.id == id },
            plateB = plateB.filterNot { it.id == id },
            bowl = bowl + token.copy(counted = true, origin = if (fromA) 1 else 2),
        )
        val k = next.bowl.size
        val beats = buildList {
            add(Beat.Play(Sfx.CLINK))
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
 * TAKE: a bowl of n tokens. The child removes exactly b of them (THUD only —
 * no counting words while removing); then the app counts what is left and
 * says the take-away fact. Taps after b removals are recorded, nothing else.
 */
data class TakeState(
    val n: Int,
    val b: Int,
    val tokens: List<Token>,
    val invalidTaps: Int = 0,
) {
    val removed: Int get() = tokens.count { it.gone }
    val left: Int get() = n - removed
    val done: Boolean get() = removed == b

    fun onTap(id: Int): Pair<TakeState, List<Beat>> {
        val token = tokens.find { it.id == id }
        if (done || token == null || token.gone) return struggle()
        val updated = tokens.map { if (it.id == id) it.copy(gone = true) else it }
        val beats = buildList {
            add(Beat.Play(Sfx.THUD))
            if (updated.count { it.gone } == b) {
                // The app counts what is LEFT, in order.
                var k = 0
                for (t in updated) if (!t.gone) add(Beat.SayCount(++k))
                add(Beat.SayFactTake(n, b, k))
                add(Beat.FlashTake(n, b, k))
                add(Beat.Confetti)
                add(Beat.Play(Sfx.CHIME))
            }
        }
        return TakeState(n, b, updated, invalidTaps) to beats
    }

    private fun struggle(): Pair<TakeState, List<Beat>> =
        copy(invalidTaps = invalidTaps + 1) to emptyList()
}
