package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptTest {

    @Test
    fun two_clean_rounds_in_a_row_level_up_and_reset_streak() {
        val start = Adapt()
        assertEquals(Adapt(0, 0), start)

        val one = start.record(invalidTaps = 0)
        assertEquals(Adapt(level = 0, streak = 1), one)

        val two = one.record(invalidTaps = 0)
        assertEquals(Adapt(level = 1, streak = 0), two)
    }

    @Test
    fun level_up_keeps_firing_every_two_clean_rounds_until_cap() {
        var a = Adapt()
        repeat(4) { a = a.record(invalidTaps = 0) } // -> level 2
        assertEquals(Adapt(level = 2, streak = 0), a)
        repeat(2) { a = a.record(invalidTaps = 0) } // capped
        assertEquals(Adapt(level = 2, streak = 0), a)
    }

    @Test
    fun three_invalid_taps_level_down_and_reset_streak() {
        val a = Adapt(level = 1, streak = 1).record(invalidTaps = 3)
        assertEquals(Adapt(level = 0, streak = 0), a)
        val b = Adapt(level = 2, streak = 0).record(invalidTaps = 7)
        assertEquals(Adapt(level = 1, streak = 0), b)
    }

    @Test
    fun level_floors_at_zero() {
        val a = Adapt(level = 0, streak = 1).record(invalidTaps = 5)
        assertEquals(Adapt(level = 0, streak = 0), a)
    }

    @Test
    fun one_slip_keeps_the_streak_motor_noise_is_not_confusion() {
        // A single accidental re-tap must not undo a clean round's progress.
        assertEquals(
            Adapt(level = 1, streak = 1),
            Adapt(level = 1, streak = 1).record(invalidTaps = 1),
        )
        assertEquals(
            Adapt(level = 0, streak = 1),
            Adapt(level = 0, streak = 1).record(invalidTaps = 1),
        )
    }

    @Test
    fun two_invalid_taps_reset_streak_only() {
        assertEquals(
            Adapt(level = 1, streak = 0),
            Adapt(level = 1, streak = 1).record(invalidTaps = 2),
        )
    }

    @Test
    fun struggle_then_two_clean_rounds_re_climb() {
        // Drop hard, then recover: the ladder eases, and it also returns.
        var a = Adapt(level = 1, streak = 1).record(invalidTaps = 4)
        assertEquals(Adapt(level = 0, streak = 0), a)
        a = a.record(invalidTaps = 0)
        assertEquals(Adapt(level = 0, streak = 1), a)
        a = a.record(invalidTaps = 0)
        assertEquals(Adapt(level = 1, streak = 0), a)
    }
}
