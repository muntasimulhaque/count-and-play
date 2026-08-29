package app.maqsadah.count_and_play.core

/** The three activities, rotating forever in [SessionState]. */
enum class Skill { COUNT, ADD, TAKE }

/** The round currently being played. */
sealed class Round {
    abstract val done: Boolean
    abstract val invalidTaps: Int

    /** Beats the host should perform when this round is dealt. */
    abstract fun startBeats(): List<Beat>

    data class IsCount(val state: CountState) : Round() {
        override val done: Boolean get() = state.done
        override val invalidTaps: Int get() = state.invalidTaps
        override fun startBeats(): List<Beat> = listOf(Beat.SayPromptCount)
    }

    data class IsAdd(val state: AddState) : Round() {
        override val done: Boolean get() = state.done
        override val invalidTaps: Int get() = state.invalidTaps
        // ADD opens with counting each plate, so it borrows the count prompt.
        override fun startBeats(): List<Beat> = listOf(Beat.SayPromptCount)
    }

    data class IsTake(val state: TakeState) : Round() {
        override val done: Boolean get() = state.done
        override val invalidTaps: Int get() = state.invalidTaps
        // TAKE opens by counting the whole tray; the subtraction ask is a
        // beat inside the round, spoken when that count completes.
        override fun startBeats(): List<Beat> = listOf(Beat.SayPromptCount)
    }
}

/**
 * Everything the core knows about one sitting. Plain data transforms: [tap]
 * and [nextRound] return a fresh state plus beats, so the host owns pacing,
 * persistence and undo. The [Adapt]s are exposed so the host can store them.
 *
 * Randomness is carried as a [seed], not a generator: two equal sessions
 * produce equal futures, so a session can be saved, copied or replayed and
 * still deal exactly what this child was dealt.
 */
data class SessionState(
    val skill: Skill,
    val round: Round,
    val seed: Long,
    val adaptCount: Adapt = Adapt(),
    val adaptAdd: Adapt = Adapt(),
    val adaptTake: Adapt = Adapt(),
) {
    fun adapt(skill: Skill): Adapt = when (skill) {
        Skill.COUNT -> adaptCount
        Skill.ADD -> adaptAdd
        Skill.TAKE -> adaptTake
    }

    /** One child tap, routed to the current round. A finished round ignores taps. */
    fun tap(id: Int): Pair<SessionState, List<Beat>> {
        if (round.done) return this to emptyList()
        val (nextRound, beats) = when (val r = round) {
            is Round.IsCount -> r.state.onTap(id).let { (s, b) -> Round.IsCount(s) to b }
            is Round.IsAdd -> r.state.onTap(id).let { (s, b) -> Round.IsAdd(s) to b }
            is Round.IsTake -> r.state.onTap(id).let { (s, b) -> Round.IsTake(s) to b }
        }
        return copy(round = nextRound) to beats
    }

    /**
     * The ADD pour button, routed to the current round; every other round
     * ignores it, and a finished round ignores it too.
     */
    fun pour(): Pair<SessionState, List<Beat>> {
        if (round.done) return this to emptyList()
        val r = round
        if (r !is Round.IsAdd) return this to emptyList()
        val (next, beats) = r.state.onPour()
        return copy(round = Round.IsAdd(next)) to beats
    }

    /**
     * Records the finished round into this skill's [Adapt] and deals the next
     * round of the same skill at the (possibly changed) level. The returned
     * beats are the new round's start beats. Safe by construction on an
     * unfinished round: it changes nothing.
     */
    fun nextRound(): Pair<SessionState, List<Beat>> {
        if (!round.done) return this to emptyList()
        val adapted = adapt(skill).record(round.invalidTaps)
        val nextSeed = successorSeed(seed)
        val next = deal(skill, adapted.level, SeededRng(nextSeed))
        val nextSession = when (skill) {
            Skill.COUNT -> copy(adaptCount = adapted)
            Skill.ADD -> copy(adaptAdd = adapted)
            Skill.TAKE -> copy(adaptTake = adapted)
        }.copy(seed = nextSeed, round = next)
        return nextSession to next.startBeats()
    }
}

/** Deals a fresh round for [skill] at [level]. */
fun deal(skill: Skill, level: Int, rng: Rng): Round = when (skill) {
    Skill.COUNT -> Round.IsCount(CountRound.next(level, rng))
    Skill.ADD -> Round.IsAdd(AddRound.next(level, rng))
    Skill.TAKE -> Round.IsTake(TakeRound.next(level, rng))
}

/**
 * Starts a fresh session for [skill] at [level] from [seed], carrying any
 * previously earned adapts. The stored seed is already the successor of the
 * one used for the first deal, so the stream never repeats a draw.
 */
fun startSession(
    skill: Skill,
    seed: Long,
    level: Int = 0,
    adaptCount: Adapt = Adapt(),
    adaptAdd: Adapt = Adapt(),
    adaptTake: Adapt = Adapt(),
): SessionState = SessionState(
    skill = skill,
    round = deal(skill, level, SeededRng(seed)),
    seed = successorSeed(seed),
    adaptCount = adaptCount,
    adaptAdd = adaptAdd,
    adaptTake = adaptTake,
)

/** A deterministic, well-mixed step in the seed stream (a 64-bit LCG). */
fun successorSeed(seed: Long): Long =
    seed * 6364136223846793005L + 1442695040888963407L
