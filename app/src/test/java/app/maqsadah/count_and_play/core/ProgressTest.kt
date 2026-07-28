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

    /**
     * Advancement used to also demand that the successes span two *different*
     * sittings. Played against the real rules, that single clause made it
     * impossible to leave the first level on the first day — so every child's
     * first sitting was seven rounds of counting to three, whatever he could
     * actually do. A home app gets one first impression, and it was spending it
     * on the easiest thing it had.
     */
    @Test
    fun `a child who can already do it moves up in his first sitting`() {
        val after = Progress().attempt(true, true, true, true)
        assertEquals("four in a row is evidence enough", 2, after.level(Skill.COUNT))
    }

    @Test
    fun `three of four promotes, and it still takes four`() {
        val three = Progress().attempt(true, false, true)
        assertEquals("nothing moves before the window is full", 1, three.level(Skill.COUNT))

        assertEquals(2, three.attempt(true).level(Skill.COUNT))
        assertEquals("two of four is not enough", 1, Progress().attempt(true, false, false, true).level(Skill.COUNT))
    }

    /**
     * `recent` is cleared on every level change, so counting it made the skill
     * that had just moved the least-practised one — and the "balanced"
     * scheduler dealt out runs of five to seven identical activities. `plays`
     * is never reset, which is the whole reason it exists.
     */
    @Test
    fun `the practice count survives a level change`() {
        val promoted = Progress().attempt(true, true, true, true)
        assertEquals(2, promoted.level(Skill.COUNT))
        assertEquals(0, promoted.skills.getValue(Skill.COUNT).recent.size)
        assertEquals("four attempts happened, whatever the window says", 4, promoted.skills.getValue(Skill.COUNT).plays)
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
