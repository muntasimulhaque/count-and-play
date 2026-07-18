package app.maqsadah.count_and_play.twa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure game logic in [G]. These run on the JVM (no Android
 * framework), so they execute with `gradle :app:testReleaseUnitTest` /
 * `testDebugUnitTest`. Randomised generators are checked over many iterations
 * to catch out-of-range or negative results.
 */
class GameLogicTest {

    private val reps = 5_000

    /* ---------- plural() ---------- */

    @Test
    fun plural_returnsPluralForNotOne() {
        for (name in G.NAMES.values) {
            assertEquals(name, G.plural(name, 0))
            assertEquals(name, G.plural(name, 2))
            assertEquals(name, G.plural(name, 20))
        }
    }

    @Test
    fun plural_returnsCorrectSingularForOne() {
        assertEquals("apple", G.plural("apples", 1))
        assertEquals("orange", G.plural("oranges", 1))
        assertEquals("mango", G.plural("mangoes", 1))
        assertEquals("banana", G.plural("bananas", 1))
        assertEquals("strawberry", G.plural("strawberries", 1))
        assertEquals("star", G.plural("stars", 1))
        assertEquals("balloon", G.plural("balloons", 1))
        assertEquals("ball", G.plural("balls", 1))
        assertEquals("car", G.plural("cars", 1))
    }

    /* ---------- word() covers every reachable number ---------- */

    @Test
    fun word_coversZeroThroughMax() {
        assertEquals("zero", G.word(0))
        assertEquals("twenty", G.word(G.MAX_N))
        for (n in 0..G.MAX_N) assertTrue(G.word(n).isNotBlank())
    }

    /* ---------- rand()/randF() bounds ---------- */

    @Test
    fun rand_staysInRange() {
        repeat(reps) {
            val r = G.rand(10)
            assertTrue(r in 0..9)
        }
    }

    @Test
    fun randF_isUnitInterval() {
        repeat(reps) {
            val r = G.randF()
            assertTrue(r >= 0.0 && r < 1.0)
        }
    }

    /* ---------- quizProblem() invariants ---------- */

    @Test
    fun quizProblem_isConsistentAndInRange() {
        for (round in 0 until 20) {
            for (correct in 0..10) {
                repeat(reps / 20) {
                    val p = G.quizProblem(round, correct)
                    // op alternates strictly by round parity
                    assertEquals(if (round % 2 == 0) "+" else "−", p.op)
                    assertTrue(p.a >= 1)
                    assertTrue(p.b >= 1)
                    val expected = if (p.op == "+") p.a + p.b else p.a - p.b
                    assertEquals(expected, p.answer)
                    assertTrue("answer must be >= 0", p.answer >= 0)
                    val cap = if (correct >= 5) 10 else 5
                    assertTrue("answer within level cap", p.answer <= cap)
                }
            }
        }
    }

    /* ---------- diceProblem() invariants ---------- */

    @Test
    fun diceProblem_staysWithinBounds() {
        repeat(reps) {
            val p = G.diceProblem()
            assertTrue(p.op == "+" || p.op == "−")
            assertTrue(p.a in 1..G.MAX_N)
            assertTrue(p.b in 1..G.MAX_N)
            val expected = if (p.op == "+") p.a + p.b else p.a - p.b
            assertEquals(expected, p.answer)
            assertTrue("never negative", p.answer >= 0)
            assertTrue("never exceeds MAX_N", p.answer <= G.MAX_N)
        }
    }

    /* ---------- answerOptions() ---------- */

    @Test
    fun answerOptions_areThreeDistinctIncludingCorrect() {
        for (correct in 0..10) {
            repeat(reps / 11) {
                val opts = G.answerOptions(correct, 10)
                assertEquals(3, opts.size)
                assertEquals("no duplicate options", 3, opts.toSet().size)
                assertTrue("correct answer present", opts.contains(correct))
                assertTrue("all options in range", opts.all { it in 0..10 })
            }
        }
    }
}
