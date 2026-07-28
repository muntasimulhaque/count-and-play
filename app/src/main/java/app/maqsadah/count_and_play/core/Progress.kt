package app.maqsadah.count_and_play.core

/** One recorded attempt. [session] lets advancement require evidence across days. */
data class Attempt(val correct: Boolean, val session: Int)

data class SkillRecord(
    val level: Int = 1,
    val recent: List<Attempt> = emptyList(),
    /**
     * Every attempt ever made at this skill. Unlike [recent] it is never reset,
     * which is the whole point: the scheduler suggests whatever has had the
     * least attention, and [recent] is cleared on every level change — so the
     * skill that had just moved was always the least-practised one and got
     * suggested again and again, five and six times in a row.
     */
    val plays: Int = 0,
)

/**
 * What the child can do, per skill.
 *
 * Deliberately *not* a single "level" number. A child can be confident counting
 * and nowhere near part-whole; one number cannot say that, and the old build's
 * single `level` is why it promoted a child to twenty in fifteen minutes.
 */
data class Progress(
    val skills: Map<Skill, SkillRecord> = emptyMap(),
    val session: Int = 0,
) {
    fun level(skill: Skill): Int = skills[skill]?.level ?: 1
    fun hasStarted(skill: Skill): Boolean = skills.containsKey(skill)
}

/**
 * The advancement rule, in one place.
 *
 * Three of the last four correct. Two consecutive misses steps back down,
 * silently. There is no failure state: stepping down is the app easing, and the
 * child is never told it happened.
 *
 * This used to also require the successes to be spread over two *different*
 * sittings, to stop a lucky streak promoting. Measured against the real rules,
 * that rule alone made it impossible to leave the first level on the first day
 * — so every child's first sitting was seven rounds of counting to three,
 * whatever he could actually do. A home app gets one first impression.
 */
object Advancement {
    const val WINDOW = 4
    const val NEEDED = 3
    const val SESSIONS_REQUIRED = 1

    fun maxLevel(skill: Skill): Int = when (skill) {
        Skill.COUNT -> 4
        Skill.GIVE_N -> 4
        Skill.COMPARE -> 4
        Skill.HIDDEN -> 4
        Skill.JOIN -> 4
        Skill.SEPARATE -> 4
    }

    fun record(progress: Progress, skill: Skill, correct: Boolean): Progress {
        val current = progress.skills[skill] ?: SkillRecord()
        val recent = (current.recent + Attempt(correct, progress.session)).takeLast(WINDOW)
        val plays = current.plays + 1

        val updated = when {
            shouldAdvance(recent, current.level, skill) ->
                SkillRecord(current.level + 1, recent = emptyList(), plays = plays)

            shouldEase(recent, current.level) ->
                SkillRecord(current.level - 1, recent = emptyList(), plays = plays)

            else -> current.copy(recent = recent, plays = plays)
        }
        return progress.copy(skills = progress.skills + (skill to updated))
    }

    private fun shouldAdvance(recent: List<Attempt>, level: Int, skill: Skill): Boolean {
        if (level >= maxLevel(skill)) return false
        if (recent.size < WINDOW) return false
        val wins = recent.filter { it.correct }
        return wins.size >= NEEDED &&
            wins.map { it.session }.distinct().size >= SESSIONS_REQUIRED
    }

    private fun shouldEase(recent: List<Attempt>, level: Int): Boolean {
        if (level <= 1) return false
        val lastTwo = recent.takeLast(2)
        return lastTwo.size == 2 && lastTwo.none { it.correct }
    }
}
