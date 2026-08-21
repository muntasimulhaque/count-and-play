package app.maqsadah.count_and_play.core

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun startSession_deals_a_fresh_round_at_the_requested_level() {
        for (skill in Skill.entries) {
            val session = startSession(skill, seed = 1L)
            assertEquals(skill, session.skill)
            assertEquals(Adapt(), session.adapt(skill))
            assertFalse(session.round.done)
            assertEquals(0, session.round.invalidTaps)
            assertWithinBounds(session.round, 0)
        }
    }

    @Test
    fun startSession_resumes_at_a_stored_level_with_stored_adapts() {
        val adapts = Triple(Adapt(2), Adapt(1), Adapt(0))
        val session = startSession(
            Skill.ADD,
            seed = 9L,
            level = 1,
            adaptCount = adapts.first,
            adaptAdd = adapts.second,
            adaptTake = adapts.third,
        )
        assertEquals(1, session.adapt(Skill.ADD).level)
        assertEquals(adapts.first, session.adapt(Skill.COUNT))
        assertEquals(adapts.third, session.adapt(Skill.TAKE))
        assertWithinBounds(session.round, 1)
    }

    @Test
    fun a_full_sitting_of_thirty_rounds_per_skill() {
        for (skill in Skill.entries) {
            var session = startSession(skill, seed = 100L + skill.ordinal)
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
        var session = startSession(Skill.TAKE, seed = 5L)
        session = playToDone(session)
        val (next, startBeats) = session.nextRound()
        val state = (next.round as Round.IsTake).state
        assertEquals(listOf(Beat.SayPromptTake(state.b)), startBeats)
    }

    @Test
    fun three_invalid_taps_in_a_round_drop_the_level() {
        var session = startSession(Skill.COUNT, seed = 7L)

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

    @Test
    fun a_tap_after_done_changes_nothing() {
        for (skill in listOf(Skill.ADD, Skill.TAKE)) {
            var session = startSession(skill, seed = 21L)
            session = playToDone(session)
            val frozen = session
            // Every token is counted or gone by now; id 1 exists in every deal.
            val (next, beats) = session.tap(1)
            assertEquals(frozen, next)
            assertTrue(beats.isEmpty())
        }
    }

    @Test
    fun pour_on_count_and_take_rounds_returns_the_same_session() {
        for (skill in listOf(Skill.COUNT, Skill.TAKE)) {
            val session = startSession(skill, seed = 33L)
            val (next, beats) = session.pour()
            assertEquals(session, next)
            assertTrue(beats.isEmpty())
        }
    }

    @Test
    fun same_seed_same_sitting_two_sessions_reproduce_each_other() {
        for (skill in Skill.entries) {
            var a = startSession(skill, seed = 77L)
            var b = startSession(skill, seed = 77L)
            repeat(5) {
                a = playToDone(a).nextRound().first
                b = playToDone(b).nextRound().first
                assertEquals(a, b)
            }
        }
    }

    @Test
    fun different_seeds_deal_different_rounds() {
        val a = startSession(Skill.COUNT, seed = 1L)
        val b = startSession(Skill.COUNT, seed = 2L)
        assertNotEquals(a.round, b.round)
    }

    @Test
    fun token_ids_are_unique_within_every_count_and_take_deal() {
        for (seed in 1L..50L) for (level in 0..2) {
            val count = CountRound.next(level, SeededRng(seed))
            assertEquals(count.tokens.size, count.tokens.map { it.id }.toSet().size)
            val take = TakeRound.next(level, SeededRng(seed + 1000L))
            assertEquals(take.tokens.size, take.tokens.map { it.id }.toSet().size)
        }
    }

    @Test
    fun the_tightest_legal_take_config_completes() {
        // n = 4, b = 3 leaves exactly one: the tightest config the tables allow.
        val start = TakeState(
            n = 4,
            b = 3,
            tokens = persistentListOf(
                Token(1, ShapeKind.BALL),
                Token(2, ShapeKind.APPLE),
                Token(3, ShapeKind.STAR),
                Token(4, ShapeKind.LEAF),
            ),
        )
        val (end, beats) = start.playOut()
        assertTrue(end.done)
        assertEquals(1, end.left)
        assertEquals(
            listOf(
                Beat.SayFactTake(4, 3, 1),
                Beat.FlashTake(4, 3, 1),
                Beat.Confetti,
                Beat.Play(Sfx.CHIME),
            ),
            beats.takeLast(4),
        )
    }

    @Test
    fun bounds_tables_keep_take_b_below_take_n_at_every_level() {
        // The invariant TakeRound.next requires: even at the smallest n there
        // must be room for at least one removal and one leftover.
        for (level in 0..Adapt.MAX_LEVEL) {
            assertTrue(
                "level=$level: takeB min must stay below takeN min",
                takeBBounds(level).first < takeNBounds(level).first,
            )
        }
    }

    @Test
    fun add_splits_cover_both_extremes_across_seeds() {
        // A future off-by-one in the split range would silently kill a==1 or b==1.
        for (level in 0..Adapt.MAX_LEVEL) {
            var sawAOne = false
            var sawBOne = false
            for (seed in 1L..200L) {
                val state = AddRound.next(level, SeededRng(seed))
                sawAOne = sawAOne || state.a == 1
                sawBOne = sawBOne || state.b == 1
            }
            assertTrue("level=$level never dealt a=1", sawAOne)
            assertTrue("level=$level never dealt b=1", sawBOne)
        }
    }
}
