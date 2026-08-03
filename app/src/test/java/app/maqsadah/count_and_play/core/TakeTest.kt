package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TakeTest {

    @Test
    fun round_start_says_the_take_prompt_and_numbers_respect_bounds() {
        for (seed in 1L..50L) for (level in 0..2) {
            val rng = SeededRng(seed)
            val state = TakeRound.next(level, rng)
            assertTrue("seed=$seed n=${state.n}", state.n in takeNBounds(level))
            assertTrue("seed=$seed b=${state.b}", state.b in takeBBounds(level))
            assertTrue("seed=$seed b=${state.b} n=${state.n}", state.b < state.n)
            assertEquals(state.n, state.tokens.size)
            assertEquals(0, state.removed)
            assertEquals(listOf(Beat.SayPromptTake(state.b)), Round.IsTake(state).startBeats())
        }
    }

    @Test
    fun remove_b_with_thuds_then_the_app_counts_what_is_left() {
        for (seed in 1L..50L) {
            val rng = SeededRng(seed)
            val start = TakeRound.next(2, rng) // level 2 lets b reach 3
            val n = start.n
            val b = start.b

            val (end, beats) = start.playOut()
            assertTrue(end.done)
            assertEquals(0, end.invalidTaps)
            assertEquals(b, end.removed)
            assertEquals(n - b, end.left)
            // Removal is THUD-only; counting words come after the last removal.
            assertEquals(List(b) { Sfx.THUD } + Sfx.CHIME, beats.sfx())
            assertEquals((1..(n - b)).toList(), beats.sayCounts())
            assertEquals(
                listOf(
                    Beat.SayFactTake(n, b, n - b),
                    Beat.FlashTake(n, b, n - b),
                    Beat.Confetti,
                    Beat.Play(Sfx.CHIME),
                ),
                beats.takeLast(4),
            )
            // Every counting word comes after the final removal THUD.
            assertTrue(
                beats.lastIndexOf(Beat.Play(Sfx.THUD)) <
                    beats.indexOfFirst { it is Beat.SayCount },
            )
        }
    }

    @Test
    fun tapping_a_removed_token_before_done_is_recorded() {
        val start = TakeRound.next(1, SeededRng(3))
        val firstId = start.tokens.first().id
        val (one, _) = start.onTap(firstId)
        val (again, beats) = one.onTap(firstId)
        assertEquals(1, again.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(one.tokens, again.tokens)
    }

    @Test
    fun tapping_after_b_removed_is_recorded_with_no_change() {
        val rng = SeededRng(11)
        val start = TakeRound.next(2, rng)
        val (done, _) = start.playOut()
        assertTrue(done.done)

        val leftover = done.tokens.first { !it.gone }
        val (after, beats) = done.onTap(leftover.id)
        assertEquals(1, after.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(done.tokens, after.tokens)
        assertTrue(after.done)
    }
}
