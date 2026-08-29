package app.maqsadah.count_and_play.core

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTest {

    @Test
    fun count_each_plate_pour_then_count_the_whole() {
        for (seed in 1L..50L) for (level in 0..2) {
            val rng = SeededRng(seed)
            val start = AddRound.next(level, rng)
            assertTrue("seed=$seed a=${start.a}", start.a >= 1)
            assertTrue("seed=$seed b=${start.b}", start.b >= 1)
            assertTrue(
                "seed=$seed total=${start.total}",
                start.total in addTotalBounds(level),
            )
            assertEquals(start.a, start.plateA.size)
            assertEquals(start.b, start.plateB.size)
            // No plate ever exceeds five: the trays are five-frames, and a
            // bigger plate cannot fit a phone at a tappable size.
            assertTrue("seed=$seed a=${start.a}", start.a <= 5)
            assertTrue("seed=$seed b=${start.b}", start.b <= 5)
            // Token ids are unique across both plates.
            assertEquals(
                start.total,
                (start.plateA + start.plateB).map { it.id }.toSet().size,
            )

            val (end, beats) = start.playOut(rng)
            assertTrue(end.done)
            assertEquals(0, end.invalidTaps)
            // Plate A's count, plate B's count, then the whole: 1..a, 1..b, 1..total.
            val expected = (1..start.a).toList() + (1..start.b).toList() + (1..start.total).toList()
            assertEquals(expected, beats.sayCounts())
            // Each finished plate names its cardinal, unless it holds exactly
            // one token: then the count word already was the cardinal.
            if (start.a > 1) assertTrue(beats.contains(Beat.SayCardinal(start.a)))
            if (start.b > 1) assertTrue(beats.contains(Beat.SayCardinal(start.b)))
            assertTrue(beats.contains(Beat.SayPromptAdd))
            assertTrue(beats.contains(Beat.SayPromptAll))
            // The card lands as the fact begins, never after it is spoken.
            assertEquals(
                listOf(
                    Beat.FlashAdd(start.a, start.b, start.total),
                    Beat.SayFactAdd(start.a, start.b, start.total),
                    Beat.Confetti,
                    Beat.Play(Sfx.CHIME),
                ),
                beats.takeLast(4),
            )
            // TICKs for the plate counts, one RUSTLE for the pour, TICKs for
            // the bowl count, then the CHIME.
            assertEquals(
                List(start.total) { Sfx.TICK } + Sfx.RUSTLE + List(start.total) { Sfx.TICK } + Sfx.CHIME,
                beats.sfx(),
            )
            assertTrue(end.poured)
            assertEquals(start.total, end.bowl.size)
            assertTrue(end.plateA.isEmpty() && end.plateB.isEmpty())
            // The bowl keeps the parts: first a from plate A, then b from B.
            assertEquals(List(start.a) { 1 } + List(start.b) { 2 }, end.bowl.map { it.origin })
            // The bowl count is in the child's tap order, 1..total.
            assertEquals((1..start.total).toList(), end.bowl.map { it.countOrder }.sorted())
        }
    }

    @Test
    fun the_left_plate_must_finish_before_the_right_wakes() {
        val start = AddState(
            a = 3,
            b = 2,
            plateA = persistentListOf(
                Token(1, ShapeKind.APPLE),
                Token(2, ShapeKind.APPLE),
                Token(3, ShapeKind.APPLE),
            ),
            plateB = persistentListOf(
                Token(4, ShapeKind.BALL),
                Token(5, ShapeKind.BALL),
            ),
        )
        // A tap on the sleeping right plate is heard softly, recorded, and
        // counts nothing: the columns are counted one at a time.
        val (locked, lockedBeats) = start.onTap(4)
        assertEquals(1, locked.invalidTaps)
        assertEquals(listOf(Sfx.TICK), lockedBeats.sfx())
        assertTrue(locked.plateB.none { it.counted })

        // Counting the left plate out unlocks the right one; each keeps its own count.
        var s = locked
        for (id in listOf(1, 2, 3)) s = s.onTap(id).first
        assertTrue(s.doneA)
        assertFalse(s.doneB)
        assertFalse(s.platesReady)
        val (unlocked, beats) = s.onTap(4)
        assertEquals(listOf(1), beats.sayCounts())
        assertEquals(listOf(Sfx.TICK), beats.sfx())

        // Chips follow each plate's own tap order.
        var end = unlocked
        for (id in listOf(5)) end = end.onTap(id).first
        assertEquals(listOf(1, 2, 3), end.plateA.map { it.countOrder })
        assertEquals(listOf(1, 2), end.plateB.map { it.countOrder })
        assertTrue(end.doneA)
        assertTrue(end.doneB)
        assertTrue(end.platesReady)
    }

    @Test
    fun the_pour_waits_for_both_plates_to_be_counted() {
        val start = AddRound.next(1, SeededRng(4))
        val (same, beats) = start.onPour()
        assertEquals(start, same)
        // The touch is never dead: an asleep button still answers with a tick.
        assertEquals(listOf(Sfx.TICK), beats.sfx())

        // Only plate A counted: still not ready.
        var s = start
        for (token in start.plateA) s = s.onTap(token.id).first
        assertEquals(start.a, s.countedA)
        assertFalse(s.platesReady)
        val (stillSame, noBeats) = s.onPour()
        assertEquals(s, stillSame)
        assertEquals(listOf(Sfx.TICK), noBeats.sfx())
    }

    @Test
    fun a_one_token_plate_finishes_without_repeating_one() {
        // 1 + 1 is a third of level-0 deals; the voice must not stutter "one ... one".
        val start = AddState(
            a = 1,
            b = 1,
            plateA = persistentListOf(Token(1, ShapeKind.STAR)),
            plateB = persistentListOf(Token(2, ShapeKind.BALL)),
        )
        val (afterA, beatsA) = start.onTap(1)
        assertEquals(listOf(1), beatsA.sayCounts())
        assertTrue(beatsA.none { it is Beat.SayCardinal })
        assertTrue(afterA.doneA)
        val (afterB, beatsB) = afterA.onTap(2)
        assertEquals(listOf(1), beatsB.sayCounts())
        assertTrue(beatsB.none { it is Beat.SayCardinal })
        assertTrue(afterB.platesReady)
    }

    @Test
    fun the_pour_moves_everyone_to_the_bowl_ready_to_be_counted_afresh() {
        val rng = SeededRng(8)
        val start = AddRound.next(2, rng)
        var s = start
        for (token in start.plateA + start.plateB) s = s.onTap(token.id).first
        assertTrue(s.platesReady)

        val (poured, beats) = s.onPour()
        assertTrue(poured.poured)
        assertEquals(start.total, poured.bowl.size)
        assertTrue(poured.plateA.isEmpty() && poured.plateB.isEmpty())
        // The plates' finished totals survive the pour: they stay worn on the
        // emptied plates while the objects live in the bowl below.
        assertTrue(poured.doneA && poured.doneB)
        // Everyone starts uncounted again: the whole is counted afresh.
        assertTrue(poured.bowl.none { it.counted })
        assertTrue(poured.bowl.all { it.countOrder == 0 })
        assertEquals(listOf(Sfx.RUSTLE), beats.sfx())
        assertTrue(beats.contains(Beat.SayPromptAll))
        // But the round is not done until the bowl is counted.
        assertFalse(poured.done)

        // A second pour changes nothing; it answers with the soft tick.
        val (again, nothing) = poured.onPour()
        assertEquals(poured, again)
        assertEquals(listOf(Sfx.TICK), nothing.sfx())
    }

    @Test
    fun retapping_a_counted_plate_token_is_a_recorded_struggle() {
        val start = AddRound.next(0, SeededRng(4))
        val firstId = start.plateA.first().id

        val (once, moveBeats) = start.onTap(firstId)
        assertEquals(listOf(1), moveBeats.sayCounts())
        assertEquals(listOf(Sfx.TICK), moveBeats.sfx())

        val (twice, beats) = once.onTap(firstId)
        assertEquals(1, twice.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(once.plateA, twice.plateA)
        assertEquals(once.plateB, twice.plateB)
    }

    @Test
    fun a_counted_bowl_token_cannot_be_counted_twice() {
        val rng = SeededRng(6)
        var s = AddRound.next(0, rng)
        for (token in s.plateA + s.plateB) s = s.onTap(token.id).first
        s = s.onPour().first

        val bowlId = s.bowl.first().id
        val (once, countBeats) = s.onTap(bowlId)
        assertEquals(listOf(1), countBeats.sayCounts())

        val (twice, beats) = once.onTap(bowlId)
        assertEquals(1, twice.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(once.bowl, twice.bowl)
    }
}
