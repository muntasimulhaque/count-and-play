package app.maqsadah.count_and_play.core

/**
 * A sitting has a shape and an end.
 *
 * The old build's guided mode was `finishRound() -> delay(1400) -> guidedRound()`
 * forever: autoplay, presented to an age group with no self-regulation. A
 * session here is a small number of tasks and then a calm, explicit finish.
 */
data class SessionState(
    val progress: Progress,
    val shape: ShapeKind,
    val index: Int = 0,
    val lastMissed: Boolean = false,
) {
    val isComplete: Boolean get() = index >= Scheduler.TASKS_PER_SESSION
}

data class Plan(val skill: Skill, val level: Int)

object Scheduler {
    /** Short enough to finish inside a 3-year-old's attention for one sitting. */
    const val TASKS_PER_SESSION = 7

    /** Every few tasks, drop below the working level so success stays frequent. */
    const val EASY_EVERY = 4

    fun plan(session: SessionState, rng: Rng): Plan {
        val skill = chooseSkill(session.progress, rng)
        val working = session.progress.level(skill)
        // After a miss the next task is deliberately easier, so the next thing
        // that happens to the child is success.
        val ease = session.lastMissed || (session.index + 1) % EASY_EVERY == 0
        return Plan(skill, if (ease) maxOf(1, working - 1) else working)
    }

    fun task(session: SessionState, rng: Rng): Task {
        val plan = plan(session, rng)
        return Ladder.taskFor(plan.skill, plan.level, session.shape, rng)
    }

    fun record(session: SessionState, result: TaskResult): SessionState =
        session.copy(
            progress = Advancement.record(session.progress, result.skill, result.correct),
            index = session.index + 1,
            lastMissed = !result.correct,
        )

    /** Ends the sitting and moves the session counter on, so advancement can
     *  require evidence from more than one day. */
    fun close(session: SessionState): Progress =
        session.progress.copy(session = session.progress.session + 1)

    /**
     * Practise whatever has had the least attention, so no strand stalls while
     * another is drilled. Ties are broken randomly rather than by enum order.
     */
    private fun chooseSkill(progress: Progress, rng: Rng): Skill {
        val available = Ladder.unlocked(progress)
        val fewest = available.minOf { progress.skills[it]?.recent?.size ?: 0 }
        val candidates = available.filter { (progress.skills[it]?.recent?.size ?: 0) == fewest }
        return rng.pick(candidates)
    }
}
