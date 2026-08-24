package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The layout solvers' promise: on any screen a real phone or tablet has, and
 * for every round the core can deal, the trays fit, the objects stay big,
 * and rows stay balanced (no lonely orphan row). ADD additionally must fit
 * BOTH phases of the game: the plates counted with an empty bowl waiting,
 * and the poured bowl counted with the emptied plates keeping their place.
 * This is what failed in v7.3 and earlier: at the top level the plates
 * towered off-screen and the bowl was pushed out of reach exactly when a
 * child advanced.
 */
class AddLayoutTest {

    /** Every (bigger plate, total) the core can deal at any level. Plates
     *  never exceed five: that is both the five-frame the trays draw and what
     *  a phone can fit at a toddler's touch size (see Rounds.kt). */
    private val deals = listOf(
        1 to 2, 2 to 3,          // level 0
        4 to 5,                  // level 1 worst split
        5 to 5, 5 to 10,         // level 2: balanced and lopsided extremes
    )

    private fun assertBothPhasesFit(
        playWidth: Dp,
        bigPlate: Int,
        total: Int,
        availHeight: Dp,
    ) {
        val s = solveAddTraySizes(playWidth, bigPlate, total, availHeight)
        val room = availHeight - PourReserve - SectionGap * 2

        val beforePour =
            trayHeight(bigPlate, s.plate, s.platePerRow) + s.bowl + TrayPad * 2 <= room
        val afterPour =
            s.plate + TrayPad * 2 + trayHeight(total, s.bowl, s.bowlPerRow, seated = true) <= room
        assertTrue(
            "beforePour overflows: w=$playWidth h=$availHeight plate=$bigPlate/$total " +
                "sizes=${s.plate}/${s.bowl}",
            beforePour,
        )
        assertTrue(
            "afterPour overflows: w=$playWidth h=$availHeight plate=$bigPlate/$total " +
                "sizes=${s.plate}/${s.bowl}",
            afterPour,
        )
        // Tokens never collapse to nothing.
        assertTrue(s.plate >= MinObject)
        assertTrue(s.bowl >= MinObject)
    }

    @Test
    fun `every dealable round fits both phases on common screens`() {
        // 360 dp is the narrowest hardware this app's minSdk 23 generation
        // shipped on; nothing 320 dp wide can install it. Heights keep a
        // phone-ish aspect: shorter ones do not exist in the wild.
        for ((width, availHeight) in listOf(
            360.dp to 640.dp, 393.dp to 675.dp,
            411.dp to 731.dp, 480.dp to 854.dp, 600.dp to 960.dp, 800.dp to 1280.dp,
        )) {
            for ((bigPlate, total) in deals) {
                assertBothPhasesFit(width, bigPlate, total, availHeight)
            }
        }
    }

    @Test
    fun `absurd screens degrade to floor sizes rather than crashing`() {
        val s = solveAddTraySizes(240.dp, 5, 10, 400.dp)
        assertTrue(s.plate >= MinObject)
        assertTrue(s.bowl >= MinObject)
    }

    @Test
    fun `a normal early round keeps objects big`() {
        val s = solveAddTraySizes(playWidth = 411.dp, bigPlate = 2, total = 3, availHeight = 640.dp)
        assertTrue("early rounds must stay generous, got ${s.plate}", s.plate >= 64.dp)
        assertTrue("early bowls must stay generous, got ${s.bowl}", s.bowl >= 64.dp)
    }

    @Test
    fun `more room never shrinks the trays`() {
        var last = solveAddTraySizes(411.dp, 5, 10, 500.dp)
        for (availHeight in 520..760 step 20) {
            val next = solveAddTraySizes(411.dp, 5, 10, availHeight.dp)
            assertTrue("plate shrank at $availHeight", next.plate >= last.plate)
            assertTrue("bowl shrank at $availHeight", next.bowl >= last.bowl)
            last = next
        }
    }

    @Test
    fun `rows stay balanced, never a lonely orphan`() {
        // Every count the trays can show arranges into rows whose last row
        // holds at least two, so no object ever sits alone under a full row.
        for (count in 2..10) {
            val perRow = perRowTemplate(count)
            val lastRow = count - perRow * (rowsFor(count, perRow) - 1)
            assertTrue("count $count leaves an orphan", lastRow >= 2)
        }
    }

    @Test
    fun `single trays use the room a phone offers`() {
        // Four becomes a big line of four, five the 3+2 five-frame, ten a
        // couple of full rows: all at sizes a small finger enjoys.
        val four = solveTray(379.dp, 4, SingleCap)
        assertEquals(4, four.perRow)
        assertTrue("line of four too small: ${four.size}", four.size >= 72.dp)

        val five = solveTray(379.dp, 5, SingleCap)
        assertEquals(3, five.perRow)
        assertTrue("five-frame too small: ${five.size}", five.size >= 96.dp)

        val ten = solveTray(379.dp, 10, SingleCap)
        assertTrue("ten too small: ${ten.size}", ten.size >= 56.dp)
        assertEquals(3, rowsFor(10, ten.perRow))
    }

    @Test
    fun `narrow trays fall back to narrower rows instead of tiny objects`() {
        // A phone-width ADD plate cannot fit three touch-sized nodes in a
        // row; it takes two per row and keeps the objects at finger size.
        val plate = solveTray(182.dp, 3, AddCap)
        assertEquals(2, plate.perRow)
        assertTrue("plate objects too small: ${plate.size}", plate.size >= 64.dp)
    }

    @Test
    fun `the geometry helper agrees with itself`() {
        // Ten seated 64 dp tokens in rows of four make three rows; an empty
        // tray reports zero rows and only its floor height.
        val rows = rowsFor(10, 4)
        assertEquals(3, rows)
        assertTrue(trayHeight(10, 64.dp, 4, seated = true) > 64.dp * rows)
        assertEquals(0, rowsFor(0, 4))
    }
}
