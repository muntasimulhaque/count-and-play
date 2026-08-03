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
        override fun startBeats(): List<Beat> = listOf(Beat.SayPromptAdd)
    }

    data class IsTake(val state: TakeState) : Round() {
        override val done: Boolean get() = state.done
        override val invalidTaps: Int get() = state.invalidTaps
        override fun startBeats(): List<Beat> = listOf(Beat.SayPromptTake(state.b))
    }
}

/**
 * Everything the core knows about one sitting. Plain data transforms: [tap]
 * and [nextRound] return a fresh state plus beats, so the host owns pacing,
 * persistence and undo. The [Adapt]s are exposed so the host can store them.
 */
data class SessionState(
    val skill: Skill,
    val round: Round,
    val rng: Rng,
    val adaptCount: Adapt = Adapt(),
    val adaptAdd: Adapt = Adapt(),
    val adaptTake: Adapt = Adapt(),
) {
    fun adapt(skill: Skill): Adapt = when (skill) {
        Skill.COUNT -> adaptCount
        Skill.ADD -> adaptAdd
        Skill.TAKE -> adaptTake
    }

    /** One child tap, routed to the current round. */
    fun tap(id: Int): Pair<SessionState, List<Beat>> {
        val (nextRound, beats) = when (val r = round) {
            is Round.IsCount -> r.state.onTap(id).let { (s, b) -> Round.IsCount(s) to b }
            is Round.IsAdd -> r.state.onTap(id).let { (s, b) -> Round.IsAdd(s) to b }
            is Round.IsTake -> r.state.onTap(id).let { (s, b) -> Round.IsTake(s) to b }
        }
        return copy(round = nextRound) to beats
    }

    /**
     * Records the finished round into this skill's [Adapt] and deals the next
     * round of the same skill at the (possibly changed) level. The returned
     * beats are the new round's start beats.
     */
    fun nextRound(): Pair<SessionState, List<Beat>> {
        val invalid = round.invalidTaps
        val adapted = adapt(skill).record(clean = invalid == 0, invalidTaps = invalid)
        val next = deal(skill, adapted.level, rng)
        val nextSession = when (skill) {
            Skill.COUNT -> copy(adaptCount = adapted)
            Skill.ADD -> copy(adaptAdd = adapted)
            Skill.TAKE -> copy(adaptTake = adapted)
        }
        return nextSession.copy(round = next) to next.startBeats()
    }
}

/** Deals a fresh round for [skill] at [level]. */
fun deal(skill: Skill, level: Int, rng: Rng): Round = when (skill) {
    Skill.COUNT -> Round.IsCount(CountRound.next(level, rng))
    Skill.ADD -> Round.IsAdd(AddRound.next(level, rng))
    Skill.TAKE -> Round.IsTake(TakeRound.next(level, rng))
}

/** Starts a fresh session for [skill] at level 0. */
fun choose(skill: Skill, rng: Rng): SessionState =
    SessionState(skill, deal(skill, 0, rng), rng)
