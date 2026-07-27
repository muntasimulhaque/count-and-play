package app.maqsadah.count_and_play.core

/** One recorded attempt. [session] lets advancement require evidence across days. */
data class Attempt(val correct: Boolean, val session: Int)

data class SkillRecord(
    val level: Int = 1,
    val recent: List<Attempt> = emptyList(),
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
 * Three of the last four correct, with the successes spread over at least two
 * *different sessions* — so a lucky streak inside one sitting never promotes.
 * Two consecutive misses steps back down, silently. There is no failure state:
 * stepping down is the app easing, and the child is never told it happened.
 */
object Advancement {
    const val WINDOW = 4
    const val NEEDED = 3
    const val SESSIONS_REQUIRED = 2

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

        val updated = when {
            shouldAdvance(recent, current.level, skill) ->
                SkillRecord(level = current.level + 1, recent = emptyList())

            shouldEase(recent, current.level) ->
                SkillRecord(level = current.level - 1, recent = emptyList())

            else -> current.copy(recent = recent)
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
