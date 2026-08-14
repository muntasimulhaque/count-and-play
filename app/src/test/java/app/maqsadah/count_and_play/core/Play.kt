package app.maqsadah.count_and_play.core

/**
 * Test harness that plays rounds the way a child who follows along would.
 * The whole point of the core layer is that this is possible: the entire game
 * is playable, start to finish, with no Android, no emulator and no clock.
 */

fun List<Beat>.sayCounts(): List<Int> = filterIsInstance<Beat.SayCount>().map { it.n }

fun List<Beat>.sfx(): List<Sfx> = filterIsInstance<Beat.Play>().map { it.sfx }

/** Taps every token once, in tray order. */
fun CountState.playOut(): Pair<CountState, List<Beat>> {
    var state = this
    val beats = mutableListOf<Beat>()
    for (token in tokens) {
        val (next, more) = state.onTap(token.id)
        state = next
        beats += more
    }
    return state to beats
}

/**
 * Plays a full ADD round the way a child who follows along would: count each
 * plate (shuffled within the plate), pour, then count the bowl (shuffled).
 */
fun AddState.playOut(rng: Rng): Pair<AddState, List<Beat>> {
    var state = this
    val beats = mutableListOf<Beat>()
    val plateOrder = rng.shuffled(plateA.map { it.id }) + rng.shuffled(plateB.map { it.id })
    for (id in plateOrder) {
        val (next, more) = state.onTap(id)
        state = next
        beats += more
    }
    val (poured, pourBeats) = state.onPour()
    state = poured
    beats += pourBeats
    for (id in rng.shuffled(state.bowl.map { it.id })) {
        val (next, more) = state.onTap(id)
        state = next
        beats += more
    }
    return state to beats
}

/** Removes the first [TakeState.b] tokens, then counts what is left, in order. */
fun TakeState.playOut(): Pair<TakeState, List<Beat>> {
    var state = this
    val beats = mutableListOf<Beat>()
    for (token in tokens.take(b)) {
        val (next, more) = state.onTap(token.id)
        state = next
        beats += more
    }
    for (token in state.tokens.filter { !it.gone }) {
        val (next, more) = state.onTap(token.id)
        state = next
        beats += more
    }
    return state to beats
}

// The difficulty bounds table, restated here on purpose: the tests assert the
// spec, not the implementation's own constants.
fun countBounds(level: Int): IntRange = when (level) {
    0 -> 1..3
    1 -> 1..5
    else -> 1..10
}

fun addTotalBounds(level: Int): IntRange = when (level) {
    0 -> 2..3
    1 -> 2..5
    else -> 2..10
}

fun takeNBounds(level: Int): IntRange = when (level) {
    0 -> 2..3
    1 -> 3..5
    else -> 4..10
}

fun takeBBounds(level: Int): IntRange = when (level) {
    0 -> 1..1
    1 -> 1..2
    else -> 1..3
}
