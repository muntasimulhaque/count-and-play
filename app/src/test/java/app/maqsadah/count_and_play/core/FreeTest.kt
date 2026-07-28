package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The free tray is the one place in the app with no question in it, so what
 * these tests mostly prove is what it never does.
 */
class FreeTest {

    private val shape = ShapeKind.BEAD

    private fun FreeState.tap(id: Int) = FreePlay.onTap(this, id)

    private fun FreeState.fill(n: Int): FreeState {
        var state = this
        repeat(n) { state = state.tap(state.tokens.inZone(Zone.SOURCE).first().id).first }
        return state
    }

    @Test
    fun `it opens as a full heap and an empty bowl`() {
        val state = FreePlay.begin(shape)
        assertEquals(MAX_COUNT, state.tokens.countIn(Zone.SOURCE))
        assertEquals(0, state.made)
    }

    @Test
    fun `every change is named, and the name is only ever the count`() {
        var state = FreePlay.begin(shape)
        val said = mutableListOf<Line>()
        repeat(4) {
            val (next, script) = state.tap(state.tokens.inZone(Zone.SOURCE).first().id)
            state = next
            said += script.lines()
        }
        assertEquals(4, state.made)
        assertEquals(
            "the tray states what is there and nothing else",
            listOf(1, 2, 3, 4),
            said.filterIsInstance<Line.Cardinal>().map { it.n },
        )
        // No instruction, no request, no verdict — there is nothing to answer.
        assertTrue(
            said.none {
                it is Line.GiveN || it is Line.CountThem || it is Line.HowMany ||
                    it is Line.TooMany || it is Line.GaveIt
            },
        )
    }

    @Test
    fun `anything put in can be taken back out, and zero is named plainly`() {
        var state = FreePlay.begin(shape).fill(3)
        assertEquals(3, state.made)

        repeat(3) {
            val (next, script) = state.tap(state.tokens.inZone(Zone.BOWL).first().id)
            state = next
            if (state.made == 0) {
                assertEquals(listOf(Line.NothingLeft), script.lines())
            }
        }
        assertEquals(0, state.made)
        assertEquals(MAX_COUNT, state.tokens.countIn(Zone.SOURCE))
    }

    @Test
    fun `the tray holds ten and the objects are conserved throughout`() {
        var state = FreePlay.begin(shape)
        repeat(40) {
            val pool = state.tokens.inZone(if (it % 3 == 2) Zone.BOWL else Zone.SOURCE)
            if (pool.isNotEmpty()) state = state.tap(pool.first().id).first
            // Nothing is ever created or destroyed, whatever he does to it.
            assertEquals(MAX_COUNT, state.tokens.size)
            assertEquals(
                MAX_COUNT,
                state.tokens.countIn(Zone.SOURCE) + state.tokens.countIn(Zone.BOWL),
            )
        }
        assertEquals(MAX_COUNT, FreePlay.begin(shape).fill(MAX_COUNT).made)
    }
}
