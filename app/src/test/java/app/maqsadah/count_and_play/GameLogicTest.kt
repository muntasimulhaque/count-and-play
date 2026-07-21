package app.maqsadah.count_and_play

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

    /* ---------- guidedProblem() invariants ---------- */

    @Test
    fun guidedProblem_isConsistentAndInRange() {
        for (round in 0 until 20) {
            for (cap in G.LEVEL_CAPS) {
                repeat(reps / 20) {
                    val p = G.guidedProblem(round, cap)
                    // op alternates strictly by round parity
                    assertEquals(if (round % 2 == 0) "+" else "−", p.op)
                    assertTrue("a >= 1", p.a >= 1)
                    assertTrue("b >= 1", p.b >= 1)
                    val expected = if (p.op == "+") p.a + p.b else p.a - p.b
                    assertEquals(expected, p.answer)
                    assertTrue("answer must be >= 0", p.answer >= 0)
                    assertTrue("answer within level cap", p.answer <= cap)
                    if (p.op == "+") {
                        assertTrue("sum within cap", p.a + p.b <= cap)
                    } else {
                        assertTrue("start within cap", p.a <= cap)
                        assertTrue("never take away more than there is", p.b <= p.a)
                    }
                }
            }
        }
    }

    @Test
    fun guidedProblem_levelOneNeverHasZeroAnswer() {
        // L1 (within 3): subtraction always leaves at least one.
        repeat(reps) {
            val p = G.guidedProblem(1, G.LEVEL_CAPS[0])   // odd round = subtraction
            assertEquals("−", p.op)
            assertTrue(p.answer >= 1)
        }
    }

    @Test
    fun guidedProblem_zeroAnswerPossibleFromLevelTwo() {
        // L2+ (within 5): "all gone" results must actually occur sometimes.
        var sawZero = false
        repeat(reps) {
            if (G.guidedProblem(1, G.LEVEL_CAPS[1]).answer == 0) sawZero = true
        }
        assertTrue("expected some zero-answer subtraction at L2", sawZero)
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
        for (maxN in listOf(3, 10)) {
            for (correct in 0..maxN) {
                repeat(reps / 11) {
                    val opts = G.answerOptions(correct, maxN)
                    assertEquals(3, opts.size)
                    assertEquals("no duplicate options", 3, opts.toSet().size)
                    assertTrue("correct answer present", opts.contains(correct))
                    assertTrue("all options in range", opts.all { it in 0..maxN })
                }
            }
        }
    }
}
