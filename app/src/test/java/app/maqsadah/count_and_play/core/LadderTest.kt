package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The curriculum's guarantees. These are the numbers the whole rebuild turns on:
 * a 3-year-old's subitizing limit is 3 and object-tracking limit is 1-2, so an
 * arithmetic task above five objects is not harder counting, it is a worse task.
 */
class LadderTest {

    private fun everyTask(action: (Skill, Int, Task) -> Unit) {
        for (skill in Skill.entries) {
            for (level in 1..Advancement.maxLevel(skill)) {
                for (seed in 0L until 300L) {
                    action(skill, level, Ladder.taskFor(skill, level, ShapeKind.APPLE, SeededRng(seed)))
                }
            }
        }
    }

    @Test
    fun `arithmetic never exceeds five, counting never exceeds ten`() {
        everyTask { _, _, task ->
            when (task) {
                is Task.CountIt -> assertTrue("counted ${task.n}", task.n in 1..MAX_COUNT)
                is Task.GiveMe -> assertTrue("gave ${task.n}", task.n in 1..MAX_TOTAL)
                is Task.WhichIsMore ->
                    assertTrue(maxOf(task.left, task.right) <= MAX_COUNT)
                is Task.UnderTheLeaf ->
                    assertTrue("total ${task.answer}", task.answer in 0..MAX_TOTAL)
                is Task.Join -> assertTrue("total ${task.answer}", task.answer <= MAX_TOTAL)
                is Task.Separate -> assertTrue("whole ${task.whole}", task.whole <= MAX_TOTAL)
            }
        }
    }

    @Test
    fun `no task is impossible or degenerate`() {
        everyTask { _, _, task ->
            assertTrue("negative answer in $task", task.answer >= 0)
            when (task) {
                is Task.GiveMe ->
                    assertTrue("pool ${task.pool} < n ${task.n}", task.pool > task.n)
                is Task.WhichIsMore -> {
                    assertTrue("no larger side in $task", task.left != task.right)
                    assertTrue(task.left >= 1 && task.right >= 1)
                }
                is Task.UnderTheLeaf -> {
                    assertTrue("nothing happens in $task", task.delta != 0)
                    assertTrue("cannot remove more than there are", task.start + task.delta >= 0)
                }
                is Task.Join -> assertTrue(task.a >= 1 && task.b >= 1)
                is Task.Separate -> assertTrue(task.take in 1..task.whole)
                is Task.CountIt -> assertTrue(task.n >= 1)
            }
        }
    }

    @Test
    fun `the ladder opens on counting alone and unlocks in order`() {
        val fresh = Progress()
        assertEquals(listOf(Skill.COUNT), Ladder.unlocked(fresh))

        val counts = fresh.at(Skill.COUNT, 2)
        assertTrue(Ladder.isUnlocked(Skill.GIVE_N, counts))
        assertTrue(Ladder.isUnlocked(Skill.COMPARE, counts))
        assertFalse("operations must wait for Give-N", Ladder.isUnlocked(Skill.HIDDEN, counts))

        val gives = counts.at(Skill.GIVE_N, 2)
        assertTrue(Ladder.isUnlocked(Skill.HIDDEN, gives))
        assertFalse("joining must wait for the hidden set", Ladder.isUnlocked(Skill.JOIN, gives))

        val hides = gives.at(Skill.COUNT, 3).at(Skill.HIDDEN, 2)
        assertTrue(Ladder.isUnlocked(Skill.JOIN, hides))
        assertFalse(Ladder.isUnlocked(Skill.SEPARATE, hides))

        assertTrue(Ladder.isUnlocked(Skill.SEPARATE, hides.at(Skill.JOIN, 2)))
    }

    @Test
    fun `counting-on is provoked by a big first part and a tiny second`() {
        val task = Ladder.taskFor(Skill.JOIN, 4, ShapeKind.STAR, SeededRng(7)) as Task.Join
        assertEquals("the second part must be the small one", 1, task.b)
        assertTrue("the first part must be worth starting from", task.a >= 2)
    }

    @Test
    fun `comparison introduces conflict trials only once ratios are close`() {
        fun spreads(level: Int) = (0L until 200L).count {
            (Ladder.taskFor(Skill.COMPARE, level, ShapeKind.BALL, SeededRng(it)) as Task.WhichIsMore).spread
        }
        assertEquals("level 1 must be winnable by looking", 0, spreads(1))
        assertEquals(0, spreads(2))
        assertTrue("conflict trials must appear later", spreads(3) > 0)
    }

    private fun Progress.at(skill: Skill, level: Int) =
        copy(skills = skills + (skill to SkillRecord(level)))
}
