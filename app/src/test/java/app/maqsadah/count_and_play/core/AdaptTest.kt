package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptTest {

    @Test
    fun two_clean_rounds_in_a_row_level_up_and_reset_streak() {
        val start = Adapt()
        assertEquals(Adapt(0, 0), start)

        val one = start.record(clean = true, invalidTaps = 0)
        assertEquals(Adapt(level = 0, streak = 1), one)

        val two = one.record(clean = true, invalidTaps = 0)
        assertEquals(Adapt(level = 1, streak = 0), two)
    }

    @Test
    fun level_up_keeps_firing_every_two_clean_rounds_until_cap() {
        var a = Adapt()
        repeat(4) { a = a.record(clean = true, invalidTaps = 0) } // -> level 2
        assertEquals(Adapt(level = 2, streak = 0), a)
        repeat(2) { a = a.record(clean = true, invalidTaps = 0) } // capped
        assertEquals(Adapt(level = 2, streak = 0), a)
    }

    @Test
    fun three_invalid_taps_level_down_and_reset_streak() {
        val a = Adapt(level = 1, streak = 1).record(clean = false, invalidTaps = 3)
        assertEquals(Adapt(level = 0, streak = 0), a)
        val b = Adapt(level = 2, streak = 0).record(clean = false, invalidTaps = 7)
        assertEquals(Adapt(level = 1, streak = 0), b)
    }

    @Test
    fun level_floors_at_zero() {
        val a = Adapt(level = 0, streak = 1).record(clean = false, invalidTaps = 5)
        assertEquals(Adapt(level = 0, streak = 0), a)
    }

    @Test
    fun one_or_two_invalid_taps_reset_streak_only() {
        assertEquals(
            Adapt(level = 1, streak = 0),
            Adapt(level = 1, streak = 1).record(clean = false, invalidTaps = 2),
        )
        assertEquals(
            Adapt(level = 1, streak = 0),
            Adapt(level = 1, streak = 1).record(clean = false, invalidTaps = 1),
        )
    }

    @Test
    fun a_clean_flag_wins_even_if_inputs_disagree() {
        // The host derives `clean` from invalidTaps == 0; this pins the
        // precedence if they ever diverge.
        assertEquals(
            Adapt(level = 0, streak = 1),
            Adapt().record(clean = true, invalidTaps = 4),
        )
    }
}
