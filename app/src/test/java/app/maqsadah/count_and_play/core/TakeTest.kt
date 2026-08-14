package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun take_away_counts_each_removal_then_the_child_counts_what_is_left() {
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
            // The removals count 1..b, then the leftovers count 1..left.
            assertEquals((1..b).toList() + (1..(n - b)).toList(), beats.sayCounts())
            // THUDs for the taken, TICKs for the counted, one CHIME.
            assertEquals(List(b) { Sfx.THUD } + List(n - b) { Sfx.TICK } + Sfx.CHIME, beats.sfx())
            // The ask to count the rest comes right after the last removal.
            val promptAt = beats.indexOf(Beat.SayPromptLeft)
            assertTrue(promptAt > 0)
            assertEquals(Beat.SayCount(b), beats[promptAt - 1])
            // No leftover is counted before the last removal THUD.
            assertTrue(
                beats.lastIndexOf(Beat.Play(Sfx.THUD)) <
                    beats.indexOfFirst { it is Beat.Play && it.sfx == Sfx.TICK },
            )
            assertEquals(
                listOf(
                    Beat.SayFactTake(n, b, n - b),
                    Beat.FlashTake(n, b, n - b),
                    Beat.Confetti,
                    Beat.Play(Sfx.CHIME),
                ),
                beats.takeLast(4),
            )
            // Taken tokens wear their take-away order; leftovers their count order.
            assertEquals((1..b).toList(), end.tokens.filter { it.gone }.map { it.countOrder })
            assertEquals((1..(n - b)).toList(), end.tokens.filter { !it.gone }.map { it.countOrder })
            assertTrue(end.tokens.all { it.gone || it.counted })
        }
    }

    @Test
    fun tapping_a_ghost_is_recorded_and_changes_nothing() {
        val start = TakeRound.next(1, SeededRng(3))
        val firstId = start.tokens.first().id
        val (one, _) = start.onTap(firstId)
        val (again, beats) = one.onTap(firstId)
        assertEquals(1, again.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(one.tokens, again.tokens)
    }

    @Test
    fun after_b_removed_taps_count_the_leftovers_instead_of_removing() {
        val start = TakeRound.next(1, SeededRng(11))
        var s = start
        for (token in start.tokens.take(start.b)) s = s.onTap(token.id).first
        assertTrue(s.removalDone)
        assertFalse(s.done)

        // A tap on a leftover now counts it — it does not remove it.
        val leftId = s.tokens.first { !it.gone }.id
        val (counted, beats) = s.onTap(leftId)
        assertEquals(listOf(1), beats.sayCounts())
        assertEquals(Sfx.TICK, beats.sfx().first())
        assertFalse(counted.tokens.first { it.id == leftId }.gone)
        assertEquals(1, counted.tokens.first { it.id == leftId }.countOrder)

        // Re-tapping the counted leftover is a recorded struggle.
        val (again, noBeats) = counted.onTap(leftId)
        assertEquals(1, again.invalidTaps)
        assertTrue(noBeats.isEmpty())
        assertEquals(counted.tokens, again.tokens)
    }
}
