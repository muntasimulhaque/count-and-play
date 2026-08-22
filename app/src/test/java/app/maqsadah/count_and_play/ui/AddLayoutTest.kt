package app.maqsadah.count_and_play.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ADD layout solver's promise: on any screen a real phone or tablet has,
 * and for every round the core can deal, BOTH phases of the game fit. The
 * plates counted with an empty bowl waiting, and the poured bowl counted with
 * the emptied plates keeping their place. This is what failed in v7.3 and
 * earlier: at the top level the plates towered off-screen and the bowl was
 * pushed out of reach exactly when a child advanced.
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
        val plateWidth = (playWidth - PlateGap) / 2

        val beforePour =
            trayHeight(plateWidth, bigPlate, s.plate) + s.bowl + TrayPad * 2 <= room
        val afterPour =
            s.plate + TrayPad * 2 + trayHeight(playWidth, total, s.bowl, seated = true) <= room
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
    fun `the geometry helper agrees with itself`() {
        // Ten seated 64 dp tokens pack into a few rows on a phone-width bowl;
        // height grows with row count; empty trays report zero rows.
        val width = 379.dp
        val rows = trayRows(width, 10, 64.dp, seated = true)
        assertTrue("expected a few packed rows, got $rows", rows in 2..4)
        assertEquals(rows, trayRows(width, 10, 64.dp, seated = true))
        assertTrue(trayHeight(width, 10, 64.dp, seated = true) > 64.dp * rows)
        assertEquals(0, trayRows(width, 0, 96.dp))
    }
}
