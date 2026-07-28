package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    /**
     * Everything is on the shelf from the first minute, and nothing can ever
     * leave it. The chained version of this was measured by playing the rules:
     * a perfect child first met adding at task 41 and never met taking-away at
     * all in eight sittings, and a child who re-tapped watched activities he
     * had already been shown *disappear* when a demotion recomputed the chain.
     */
    @Test
    fun `every activity is available from the very start and never withdrawn`() {
        assertEquals(Skill.entries.toList(), Ladder.unlocked(Progress()))

        for (skill in Skill.entries) {
            assertTrue("$skill must be there on day one", Ladder.isUnlocked(skill, Progress()))
        }

        // Demotion eases the numbers, never the shelf.
        val struggling = Progress().at(Skill.COUNT, 1).at(Skill.JOIN, 1)
        assertEquals(Skill.entries.toList(), Ladder.unlocked(struggling))
    }

    @Test
    fun `the child's own choice of number is bounded by where he is`() {
        // He picks; the level decides how far the choice reaches. It can never
        // put him past the range where a quantity is still visible to him.
        assertEquals(1..5, Ladder.pickRange(Skill.COUNT, 1))
        assertEquals(1..MAX_COUNT, Ladder.pickRange(Skill.COUNT, 4))
        assertEquals(1..3, Ladder.pickRange(Skill.GIVE_N, 1))

        // Whatever he picks first, the second choice cannot push a total past
        // MAX_TOTAL, and cannot ask him to take out more than there are.
        for (first in Ladder.pickRange(Skill.JOIN, 4)) {
            val second = Ladder.secondPickRange(Skill.JOIN, first)
            assertTrue("$first + ${second.last} escapes MAX_TOTAL", first + second.last <= MAX_TOTAL)
            assertTrue(second.first >= 1)
        }
        for (whole in Ladder.pickRange(Skill.SEPARATE, 4)) {
            assertEquals(1..whole, Ladder.secondPickRange(Skill.SEPARATE, whole))
        }

        // Comparison and the hidden set are relations the child does not set.
        assertTrue(Ladder.pickRange(Skill.COMPARE, 1).isEmpty())
        assertTrue(Ladder.pickRange(Skill.HIDDEN, 1).isEmpty())
    }

    @Test
    fun `a task built from the child's own numbers is the task he described`() {
        val join = Ladder.taskFrom(Skill.JOIN, ShapeKind.STAR, 3, 2) as Task.Join
        assertEquals(3, join.a)
        assertEquals(2, join.b)
        assertEquals(5, join.answer)

        val take = Ladder.taskFrom(Skill.SEPARATE, ShapeKind.STAR, 4, 3) as Task.Separate
        assertEquals(4, take.whole)
        assertEquals(3, take.take)

        // The heap he draws from always holds more than he asked for, so
        // "put three in" never collapses into "put them all in".
        val give = Ladder.taskFrom(Skill.GIVE_N, ShapeKind.STAR, 3, 0) as Task.GiveMe
        assertTrue("the heap must offer a real choice", give.pool > give.n)
    }

    /**
     * The child now builds the problem himself, so every combination his taps
     * can produce has to be a real, finishable task — including the awkward
     * ends of each range, which is exactly where a hand-picked example wouldn't
     * have looked.
     */
    @Test
    fun `every task the child can build with his own taps plays to the end`() {
        var built = 0
        for (skill in listOf(Skill.COUNT, Skill.GIVE_N, Skill.JOIN, Skill.SEPARATE)) {
            for (level in 1..Advancement.maxLevel(skill)) {
                for (first in Ladder.pickRange(skill, level)) {
                    val second = Ladder.secondPickRange(skill, first)
                    for (n in if (second.isEmpty()) listOf(0) else second.toList()) {
                        val task = Ladder.taskFrom(skill, ShapeKind.BALL, first, n)
                        assertTrue("$task exceeds what a 3-year-old can hold", task.answer <= MAX_COUNT)

                        val played = Play.task(task, Progress())
                        assertEquals("$task never finished", Step.Finished, played.state.step)
                        assertNotNull("$task recorded nothing", played.results.firstOrNull())
                        built++
                    }
                }
            }
        }
        assertTrue("the picker must actually offer choices", built > 100)
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
