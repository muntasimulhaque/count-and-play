package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TakeTest {

    @Test
    fun round_start_opens_with_counting_the_whole_and_numbers_respect_bounds() {
        for (seed in 1L..50L) for (level in 0..2) {
            val rng = SeededRng(seed)
            val state = TakeRound.next(level, rng)
            assertTrue("seed=$seed n=${state.n}", state.n in takeNBounds(level))
            assertTrue("seed=$seed b=${state.b}", state.b in takeBBounds(level))
            assertTrue("seed=$seed b=${state.b} n=${state.n}", state.b < state.n)
            assertEquals(state.n, state.tokens.size)
            assertEquals(0, state.removed)
            // The subtraction ask waits until the whole tray has been counted.
            assertFalse(state.totalDone)
            assertEquals(listOf(Beat.SayPromptCount), Round.IsTake(state).startBeats())
        }
    }

    @Test
    fun count_the_whole_take_it_away_then_count_what_is_left() {
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
            // Three separate counts, in order: the whole 1..n, the removals
            // 1..b, the leftovers 1..left.
            assertEquals((1..n).toList() + (1..b).toList() + (1..(n - b)).toList(), beats.sayCounts())
            // TICKs for the whole, THUDs for the taken, TICKs for the left,
            // then one CHIME: the sfx order alone proves the phase order.
            assertEquals(
                List(n) { Sfx.TICK } + List(b) { Sfx.THUD } + List(n - b) { Sfx.TICK } + Sfx.CHIME,
                beats.sfx(),
            )
            // The subtraction ask comes right after the whole is counted,
            // its cardinal named first (n is never 1 at any level).
            val takeAt = beats.indexOf(Beat.SayPromptTake(b))
            assertTrue(takeAt > 0)
            assertEquals(Beat.SayCardinal(n), beats[takeAt - 1])
            assertEquals(Beat.SayCount(n), beats[takeAt - 2])
            // The ask to count the rest comes right after the last removal.
            val promptAt = beats.indexOf(Beat.SayPromptLeft)
            assertTrue(promptAt > takeAt)
            assertEquals(Beat.SayCount(b), beats[promptAt - 1])
            // The card lands as the fact begins, never after it is spoken.
            assertEquals(
                listOf(
                    Beat.FlashTake(n, b, n - b),
                    Beat.SayFactTake(n, b, n - b),
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
    fun removal_waits_until_the_whole_has_been_counted() {
        val start = TakeRound.next(0, SeededRng(5))
        val (after, beats) = start.onTap(start.tokens.first().id)
        // The first tap counts, it does not remove: no THUD, nothing gone.
        assertFalse(after.totalDone)
        assertFalse(after.tokens.first().gone)
        assertEquals(1, after.tokens.first().countOrder)
        assertEquals(listOf(Sfx.TICK), beats.sfx())
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
        for (token in start.tokens) s = s.onTap(token.id).first // count the whole
        assertTrue(s.totalDone)
        for (token in start.tokens.take(start.b)) s = s.onTap(token.id).first
        assertTrue(s.removalDone)
        assertFalse(s.done)

        // A tap on a leftover now counts it; it does not remove it.
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

    @Test
    fun finishing_the_whole_count_hands_the_chips_back_and_asks_the_subtraction() {
        val start = TakeRound.next(1, SeededRng(7))
        var s = start
        var finishBeats = emptyList<Beat>()
        for (token in start.tokens) {
            val (next, beats) = s.onTap(token.id)
            s = next
            finishBeats = beats
        }
        // The count completed: the take-away ask is spoken over the reset.
        assertTrue(s.totalDone)
        assertEquals(Beat.SayPromptTake(s.b), finishBeats.last())
        // Every chip handed its number back, so the take and the left count
        // can each wear their own.
        assertTrue(s.tokens.none { it.counted })
        assertTrue(s.tokens.all { it.countOrder == 0 })
        assertEquals(0, s.removed)
    }
}
