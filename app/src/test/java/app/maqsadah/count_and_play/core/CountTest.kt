package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountTest {

    @Test
    fun perfect_round_counts_one_to_n_then_celebrates() {
        for (seed in 1L..50L) for (level in 0..2) {
            val rng = SeededRng(seed)
            val start = CountRound.next(level, rng)
            val n = start.n
            assertTrue("seed=$seed level=$level n=$n", n in countBounds(level))
            assertEquals(n, start.tokens.size)
            assertEquals(0, start.invalidTaps)

            val (end, beats) = start.playOut()
            assertTrue(end.done)
            assertEquals(0, end.invalidTaps)
            assertEquals((1..n).toList(), beats.sayCounts())
            assertEquals(
                listOf(
                    Beat.SayCardinal(n),
                    Beat.FlashCount(n),
                    Beat.Confetti,
                    Beat.Play(Sfx.CHIME),
                ),
                beats.takeLast(4),
            )
            // One TICK per tap, then the CHIME — no other sounds.
            assertEquals(List(n) { Sfx.TICK } + Sfx.CHIME, beats.sfx())
        }
    }

    @Test
    fun retapping_a_counted_token_is_recorded_but_changes_nothing() {
        for (seed in 1L..20L) {
            val rng = SeededRng(seed)
            val start = CountRound.next(0, rng)
            val firstId = start.tokens.first().id

            val (once, beatsOnce) = start.onTap(firstId)
            assertEquals(listOf(1), beatsOnce.sayCounts())
            // When n == 1 the very first tap also completes the round.
            val expectedSfx = if (start.n == 1) listOf(Sfx.TICK, Sfx.CHIME) else listOf(Sfx.TICK)
            assertEquals(expectedSfx, beatsOnce.sfx())
            assertEquals(0, once.invalidTaps)

            val (twice, beatsTwice) = once.onTap(firstId)
            assertEquals(1, twice.invalidTaps)
            assertTrue(beatsTwice.isEmpty())
            assertEquals(once.tokens, twice.tokens)
            assertEquals(once.done, twice.done)

            val (thrice, beatsThrice) = twice.onTap(firstId)
            assertEquals(2, thrice.invalidTaps)
            assertTrue(beatsThrice.isEmpty())
        }
    }

    @Test
    fun chips_follow_the_childs_tap_order_not_the_tray_order() {
        val start = CountState(
            tokens = listOf(
                Token(1, ShapeKind.APPLE),
                Token(2, ShapeKind.APPLE),
                Token(3, ShapeKind.APPLE),
                Token(4, ShapeKind.APPLE),
            ),
        )
        // Break the left-to-right habit: last first, then first, then third.
        val (s1, b1) = start.onTap(4)
        val (s2, b2) = s1.onTap(1)
        val (s3, b3) = s2.onTap(3)
        assertEquals(listOf(1), b1.sayCounts())
        assertEquals(listOf(2), b2.sayCounts())
        assertEquals(listOf(3), b3.sayCounts())
        assertEquals(1, s3.tokens.first { it.id == 4 }.countOrder)
        assertEquals(2, s3.tokens.first { it.id == 1 }.countOrder)
        assertEquals(3, s3.tokens.first { it.id == 3 }.countOrder)
        assertEquals(0, s3.tokens.first { it.id == 2 }.countOrder)
        assertFalse(s3.done)
        assertFalse(s3.tokens.first { it.id == 2 }.counted)
    }

    @Test
    fun tapping_something_that_is_not_there_is_recorded_gently() {
        val start = CountRound.next(0, SeededRng(9))
        val (after, beats) = start.onTap(-123)
        assertEquals(1, after.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(start.tokens, after.tokens)
    }
}
