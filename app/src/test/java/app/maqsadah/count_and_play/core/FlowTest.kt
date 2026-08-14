package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowTest {

    /** The next tap a child who follows along would make. */
    private fun nextTapId(round: Round): Int = when (round) {
        is Round.IsCount -> round.state.tokens.first { !it.counted }.id
        is Round.IsAdd -> with(round.state) {
            if (poured) bowl.first { !it.counted }.id
            else (plateA + plateB).first { !it.counted }.id
        }
        is Round.IsTake -> with(round.state) {
            if (removalDone) tokens.first { !it.gone && !it.counted }.id
            else tokens.first { !it.gone }.id
        }
    }

    private fun playToDone(session: SessionState): SessionState {
        var s = session
        var steps = 0
        while (!s.round.done) {
            val add = (s.round as? Round.IsAdd)?.state
            s = if (add != null && add.platesReady && !add.poured) {
                s.pour().first
            } else {
                s.tap(nextTapId(s.round)).first
            }
            check(++steps < 60) { "round did not terminate" }
        }
        return s
    }

    private fun assertWithinBounds(round: Round, level: Int) {
        when (round) {
            is Round.IsCount ->
                assertTrue("n=${round.state.n} level=$level", round.state.n in countBounds(level))
            is Round.IsAdd -> {
                assertTrue(round.state.a >= 1)
                assertTrue(round.state.b >= 1)
                assertTrue(
                    "total=${round.state.total} level=$level",
                    round.state.total in addTotalBounds(level),
                )
            }
            is Round.IsTake -> {
                assertTrue("n=${round.state.n} level=$level", round.state.n in takeNBounds(level))
                assertTrue("b=${round.state.b} level=$level", round.state.b in takeBBounds(level))
                assertTrue(round.state.b < round.state.n)
            }
        }
    }

    @Test
    fun choose_starts_a_fresh_level_zero_round() {
        val rng = SeededRng(1)
        for (skill in Skill.entries) {
            val session = choose(skill, rng)
            assertEquals(skill, session.skill)
            assertEquals(Adapt(), session.adapt(skill))
            assertFalse(session.round.done)
            assertEquals(0, session.round.invalidTaps)
            assertWithinBounds(session.round, 0)
        }
    }

    @Test
    fun a_full_sitting_of_thirty_rounds_per_skill() {
        val rng = SeededRng(42)
        for (skill in Skill.entries) {
            var session = choose(skill, rng)
            repeat(30) { roundIndex ->
                assertFalse(session.round.done)
                assertWithinBounds(session.round, session.adapt(skill).level)

                session = playToDone(session)
                assertEquals(0, session.round.invalidTaps) // played perfectly

                val (next, startBeats) = session.nextRound()
                session = next

                // Perfect play levels up every two clean rounds, capped at 2.
                assertEquals(minOf(2, (roundIndex + 1) / 2), session.adapt(skill).level)
                assertTrue(session.adapt(skill).level in 0..2)
                assertEquals(skill, session.skill)
                assertFalse(session.round.done)
                assertWithinBounds(session.round, session.adapt(skill).level)
                // nextRound hands back the new round's start beats.
                assertEquals(session.round.startBeats(), startBeats)
            }
            assertEquals(2, session.adapt(skill).level)
        }
    }

    @Test
    fun nextRound_says_the_take_prompt_for_the_new_round() {
        val rng = SeededRng(5)
        var session = choose(Skill.TAKE, rng)
        session = playToDone(session)
        val (next, startBeats) = session.nextRound()
        val state = (next.round as Round.IsTake).state
        assertEquals(listOf(Beat.SayPromptTake(state.b)), startBeats)
    }

    @Test
    fun three_invalid_taps_in_a_round_drop_the_level() {
        val rng = SeededRng(7)
        var session = choose(Skill.COUNT, rng)

        // Two clean rounds bring COUNT to level 1.
        repeat(2) {
            session = playToDone(session).nextRound().first
        }
        assertEquals(1, session.adapt(Skill.COUNT).level)

        // Now struggle: one good tap, three re-taps, then finish the round.
        val firstId = (session.round as Round.IsCount).state.tokens.first().id
        session = session.tap(firstId).first
        repeat(3) { session = session.tap(firstId).first }
        assertEquals(3, session.round.invalidTaps)
        session = playToDone(session)

        session = session.nextRound().first
        assertEquals(0, session.adapt(Skill.COUNT).level)
        assertWithinBounds(session.round, 0)
    }
}
