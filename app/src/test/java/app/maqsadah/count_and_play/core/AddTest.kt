package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTest {

    @Test
    fun counting_on_across_plates_in_shuffled_order() {
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
            assertEquals(start.total, start.plateA.size + start.plateB.size)
            // Token ids are unique across both plates.
            assertEquals(
                start.total,
                (start.plateA + start.plateB).map { it.id }.toSet().size,
            )

            val (end, beats) = start.playOut(rng)
            assertTrue(end.done)
            assertEquals(0, end.invalidTaps)
            assertEquals((1..start.total).toList(), beats.sayCounts())
            assertEquals(
                listOf(
                    Beat.SayFactAdd(start.a, start.b, start.total),
                    Beat.FlashAdd(start.a, start.b, start.total),
                    Beat.Confetti,
                    Beat.Play(Sfx.CHIME),
                ),
                beats.takeLast(4),
            )
            // One CLINK per move, then the CHIME.
            assertEquals(List(start.total) { Sfx.CLINK } + Sfx.CHIME, beats.sfx())
            assertEquals(start.total, end.bowl.size)
            assertTrue(end.plateA.isEmpty() && end.plateB.isEmpty())
        }
    }

    @Test
    fun tapping_the_bowl_is_recorded_and_changes_nothing() {
        val rng = SeededRng(4)
        val start = AddRound.next(0, rng)
        val firstId = start.plateA.first().id

        val (moved, moveBeats) = start.onTap(firstId)
        assertEquals(listOf(1), moveBeats.sayCounts())
        assertEquals(listOf(Sfx.CLINK), moveBeats.sfx())
        assertEquals(1, moved.bowl.size)

        // The same token now sits in the bowl: tapping it again is a struggle.
        val (again, beats) = moved.onTap(firstId)
        assertEquals(1, again.invalidTaps)
        assertTrue(beats.isEmpty())
        assertEquals(moved.plateA, again.plateA)
        assertEquals(moved.plateB, again.plateB)
        assertEquals(moved.bowl, again.bowl)
    }
}
