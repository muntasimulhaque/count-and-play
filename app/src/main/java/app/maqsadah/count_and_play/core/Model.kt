package app.maqsadah.count_and_play.core

/**
 * Where a countable object currently sits. The containers are deliberately the
 * same furniture for every activity: addition pours two dishes into one bowl,
 * subtraction pours one bowl into a dish. A child who plays both meets one
 * structure, not two.
 */
enum class Zone {
    /** Blue dish — the first part. */
    DISH_A,

    /** Orange dish — the second part, and the destination of a take-away. */
    DISH_B,

    /** The wide bowl — the whole. */
    BOWL,

    /** The loose heap a child draws from when producing a set. */
    SOURCE,

    /** The frame where the child builds an answer or a prediction. */
    ANSWER,

    /** The small heap a prediction is built from. Kept separate from [SOURCE]. */
    RESERVE,

    /** Under the leaf: present and countable, but not visible. */
    HIDDEN,
}

sealed interface TokenState {
    data object Idle : TokenState

    /** Counted, carrying the ordinal the child gave it. Never cleared mid-round. */
    data class Counted(val ordinal: Int) : TokenState
}

/**
 * One countable object.
 *
 * [origin] is the load-bearing field: it records which part a token came from and
 * is *never* rewritten by a join. That is how the parts stay visibly inside the
 * whole — the single thing the old build erased at exactly the moment it mattered.
 */
data class Token(
    val id: Int,
    val shape: ShapeKind,
    val zone: Zone,
    val slot: Int,
    val state: TokenState = TokenState.Idle,
    val origin: Zone = zone,
) {
    val isCounted: Boolean get() = state is TokenState.Counted
    val ordinal: Int? get() = (state as? TokenState.Counted)?.ordinal
}

fun List<Token>.inZone(zone: Zone): List<Token> = filter { it.zone == zone }

fun List<Token>.countIn(zone: Zone): Int = count { it.zone == zone }

/** Re-slots a zone's tokens into 0..n-1 in their current order (the compact-left step). */
fun List<Token>.compact(zone: Zone): List<Token> {
    val ordered = inZone(zone).sortedBy { it.slot }
    val newSlots = ordered.withIndex().associate { (i, t) -> t.id to i }
    return map { t -> newSlots[t.id]?.let { t.copy(slot = it) } ?: t }
}

/** Moves [ids] into [zone], appending them after whatever is already there. */
fun List<Token>.moveTo(ids: Set<Int>, zone: Zone): List<Token> {
    var next = countIn(zone)
    return map { t ->
        if (t.id in ids) t.copy(zone = zone, slot = next++) else t
    }
}
