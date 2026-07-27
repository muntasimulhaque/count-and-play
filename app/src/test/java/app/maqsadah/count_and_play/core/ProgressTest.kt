package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The old build called `awardStar()` unconditionally on round completion, so a
 * child who had never held a number in his head reached twenty in about fifteen
 * minutes. Advancement here has to mean something.
 */
class ProgressTest {

    private fun Progress.attempt(vararg correct: Boolean): Progress =
        correct.fold(this) { p, c -> Advancement.record(p, Skill.COUNT, c) }

    private fun Progress.nextSession() = copy(session = session + 1)

    @Test
    fun `a streak inside one sitting does not promote`() {
        val after = Progress().attempt(true, true, true, true)
        assertEquals("four in a row on one day is not evidence", 1, after.level(Skill.COUNT))
    }

    @Test
    fun `three of four across two sessions promotes`() {
        val day1 = Progress().attempt(true, true)
        val day2 = day1.nextSession().attempt(true)
        assertEquals(1, day2.level(Skill.COUNT))

        val fourth = day2.attempt(true)
        assertEquals("3 of 4 spanning two days should promote", 2, fourth.level(Skill.COUNT))
    }

    @Test
    fun `a promotion clears the window so it cannot immediately re-promote`() {
        val promoted = Progress().attempt(true, true).nextSession().attempt(true, true)
        assertEquals(2, promoted.level(Skill.COUNT))
        assertEquals(0, promoted.skills.getValue(Skill.COUNT).recent.size)
    }

    @Test
    fun `two misses in a row eases back down, silently`() {
        val start = Progress(skills = mapOf(Skill.COUNT to SkillRecord(level = 3)))
        assertEquals(2, start.attempt(false, false).level(Skill.COUNT))
    }

    @Test
    fun `the first level is a floor - a child is never pushed below the start`() {
        assertEquals(1, Progress().attempt(false, false, false, false).level(Skill.COUNT))
    }

    @Test
    fun `a miss between successes does not ease`() {
        val start = Progress(skills = mapOf(Skill.COUNT to SkillRecord(level = 3)))
        assertEquals(3, start.attempt(false, true, false).level(Skill.COUNT))
    }

    @Test
    fun `progress is tracked per skill, not as one number`() {
        var p = Progress()
        repeat(2) { p = Advancement.record(p, Skill.COUNT, true) }
        p = p.copy(session = 1)
        repeat(2) { p = Advancement.record(p, Skill.COUNT, true) }

        assertEquals(2, p.level(Skill.COUNT))
        assertEquals("an untouched skill stays at the start", 1, p.level(Skill.JOIN))
    }

    @Test
    fun `no skill can be promoted past its ceiling`() {
        var p = Progress(skills = mapOf(Skill.COUNT to SkillRecord(level = Advancement.maxLevel(Skill.COUNT))))
        repeat(20) {
            p = Advancement.record(p, Skill.COUNT, true).copy(session = p.session + 1)
        }
        assertEquals(Advancement.maxLevel(Skill.COUNT), p.level(Skill.COUNT))
    }
}
