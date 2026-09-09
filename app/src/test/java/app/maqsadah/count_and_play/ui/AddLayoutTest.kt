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
 * BOTH phases of the game: the plates counted with the sleeping bowl waiting
 * beneath them, and the poured bowl that either already occupied those
 * seats (roomy screens: nothing moves but the pieces) or rose in after the
 * plates folded (tight screens). TAKE must fit its tray, the equation above
 * it and the taken-away box below it, all at one shared object size.
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

    /**
     * Real play areas, not raw screens: width is the screen minus the 16 dp
     * gutter each side, height is the screen minus the prompt row and a
     * worst-case system inset. These are the shapes the app actually ships on,
     * including the CI capture profiles (411x731 phone, 600x960 tablet 7,
     * 800x562 tablet 10 in landscape) and the narrowest phone it installs on.
     */
    private val screens = listOf(
        328.dp to 554.dp,    // 360x640 phone: the narrowest this app installs on
        361.dp to 765.dp,    // 393x851 phone (Pixel 4a)
        379.dp to 645.dp,    // 411x731 phone (the CI phone profile)
        568.dp to 874.dp,    // 600x960 tablet 7" (the CI tablet 7 profile)
        768.dp to 470.dp,    // 800x562 tablet 10" landscape (the CI tablet 10 profile)
        768.dp to 1190.dp,   // 800x1280 tablet portrait
    )

    private fun assertAddPhasesFit(playWidth: Dp, bigPlate: Int, total: Int, availHeight: Dp) {
        val s = solveAddTraySizes(playWidth, bigPlate, total, availHeight)

        // Phase one always stands the full columns beside the sleeping bowl.
        // The plates pack at their own smaller touch floor (see TrayMath).
        assertTrue(
            "beforePour overflows: w=$playWidth h=$availHeight plate=$bigPlate/$total " +
                "sizes=${s.plate}/${s.bowl}",
            trayHeight(bigPlate, s.plate, s.platePerRow, minNode = PlateHitTarget) +
                s.bowlBefore <= availHeight,
        )
        if (s.bowlInPlace) {
            // The sleeping bowl reserves its full seated height: the seats he
            // counts the plates beside are exactly the seats the pour fills.
            assertEquals(
                "in-place bowl reserve is not the bowl's own height",
                trayHeight(total, s.bowl, s.bowlPerRow, seated = true),
                s.bowlBefore,
            )
        }
        if (s.plateAfter == s.plate) {
            // Full columns kept after the pour: the bowl slides beneath them.
            assertTrue(
                "afterPour overflows with standing plates: w=$playWidth h=$availHeight plate=$bigPlate/$total",
                trayHeight(bigPlate, s.plate, s.platePerRow, minNode = PlateHitTarget) + SectionGap * 2 +
                    trayHeight(total, s.bowl, s.bowlPerRow, seated = true) <= availHeight,
            )
        } else {
            // Tight screen: the poured plates fold into slim places wearing
            // their totals, freeing their height for the bowl.
            assertTrue(
                "folded afterPour overflows: w=$playWidth h=$availHeight plate=$bigPlate/$total",
                PouredPlatePlace + TrayPad * 2 + SectionGap * 2 +
                    trayHeight(total, s.bowl, s.bowlPerRow, seated = true) <= availHeight,
            )
        }
        // Tokens never collapse to nothing.
        assertTrue(s.plate >= MinObject)
        assertTrue(s.bowl >= MinObject)
    }

    private fun assertTakeFits(playWidth: Dp, n: Int, gone: Int, availHeight: Dp) {
        val s = solveTakeSizes(playWidth, n, gone, availHeight)
        val need =
            trayHeight(n, s.size, s.mainPerRow) + trayHeight(maxOf(gone, 1), s.size, s.takenPerRow)
        assertTrue(
            "take overflows: w=$playWidth h=$availHeight n=$n gone=$gone size=${s.size} need=$need",
            need <= availHeight - TakeEqReserve - SectionGap * 2,
        )
        assertTrue(s.size >= MinObject)
    }

    @Test
    fun `every dealable round fits both phases on common screens`() {
        for ((width, availHeight) in screens) {
            for ((bigPlate, total) in deals) {
                assertAddPhasesFit(width, bigPlate, total, availHeight)
            }
        }
    }

    @Test
    fun `plates never degenerate into a one-wide column`() {
        // A plate of five stacked one-per-row is not a five-frame, and on a
        // 360 dp phone that is exactly what a 72 dp touch floor produced. The
        // plate floor (PlateHitTarget) must keep two per row wherever two
        // objects can physically fit.
        for ((width, availHeight) in screens) {
            for ((bigPlate, total) in deals) {
                if (bigPlate < 2) continue
                val s = solveAddTraySizes(width, bigPlate, total, availHeight)
                assertTrue(
                    "plate stacks one-wide: w=$width h=$availHeight plate=$bigPlate/$total perRow=${s.platePerRow}",
                    s.platePerRow >= 2,
                )
            }
        }
    }

    @Test
    fun `solved plate rows fit their plate at the plate touch floor`() {
        // The composable enforces the same floor the solver plans against
        // (ObjectView's touchTarget): if these ever disagree, a row measures
        // wider than the plate and the tray silently overflows its well.
        for ((width, availHeight) in screens) {
            val inner = (width - PlateGap) / 2 - TrayPad * 2
            for ((bigPlate, total) in deals) {
                val s = solveAddTraySizes(width, bigPlate, total, availHeight)
                val node = nodeOf(s.plate, seated = false, minNode = PlateHitTarget)
                val row = node * s.platePerRow + TrayGap * (s.platePerRow - 1)
                assertTrue(
                    "plate row ${row} wider than inner ${inner}: w=$width plate=$bigPlate/$total",
                    row <= inner + 0.01.dp,
                )
            }
        }
    }

    @Test
    fun `the take equation fits inside the space reserved for it`() {
        // The equation is a Row of Texts; without an explicit line box Baloo's
        // default leading made it taller than TakeEqReserve, and on a short
        // screen the taken box ran past the bottom of the play area.
        assertTrue(EquationLine >= SizeEquation.value.dp * 1.05f)
        assertTrue(TakeEqReserve >= EquationLine * MAX_FONT_SCALE)
    }

    @Test
    fun `every take round fits tray equation and taken box on common screens`() {
        for ((width, availHeight) in screens) {
            for ((n, b) in listOf(3 to 1, 5 to 2, 5 to 3, 10 to 1, 10 to 3)) {
                for (gone in 0..b) assertTakeFits(width, n, gone, availHeight)
            }
        }
    }

    @Test
    fun `squat and tall screens alike keep both phases fitted`() {
        // 520 dp is about as short as a real 16:10-and-taller phone gets; the
        // 360x480 rectangle below it is a tablet-aspect relic no child holds.
        for (availHeight in 520..800 step 20) {
            for (width in listOf(360.dp, 411.dp)) {
                for ((bigPlate, total) in deals) {
                    assertAddPhasesFit(width, bigPlate, total, availHeight.dp)
                }
            }
        }
    }

    @Test
    fun `absurd screens degrade to floor sizes rather than crashing`() {
        val s = solveAddTraySizes(240.dp, 5, 10, 400.dp)
        assertTrue(s.plate >= MinObject)
        assertTrue(s.bowl >= MinObject)
        val t = solveTakeSizes(240.dp, 10, 3, 400.dp)
        assertTrue(t.size >= MinObject)
    }

    @Test
    fun `a normal early round keeps objects big`() {
        val s = solveAddTraySizes(playWidth = 411.dp, bigPlate = 2, total = 3, availHeight = 640.dp)
        assertTrue("early rounds must stay generous, got ${s.plate}", s.plate >= 64.dp)
        assertTrue("early bowls must stay generous, got ${s.bowl}", s.bowl >= 64.dp)
    }

    @Test
    fun `roomy screens keep the poured columns at full size`() {
        for ((width, availHeight) in listOf(
            379.dp to 645.dp, 568.dp to 874.dp, 768.dp to 1190.dp,
        )) {
            for ((bigPlate, total) in deals) {
                val s = solveAddTraySizes(width, bigPlate, total, availHeight)
                assertEquals(
                    "w=$width h=$availHeight deal=$bigPlate+$total folded the plates",
                    s.plate,
                    s.plateAfter,
                )
            }
        }
    }

    @Test
    fun `tight phones fold the poured plates only when physics demands`() {
        val s = solveAddTraySizes(328.dp, 5, 10, 554.dp)
        assertTrue(s.plateAfter <= PouredPlatePlace)
        assertTrue(
            s.plateAfter + TrayPad * 2 + SectionGap * 2 +
                trayHeight(10, s.bowl, s.bowlPerRow, seated = true) <= 554.dp,
        )
    }

    @Test
    fun `the finished plate's badge stays a tag, never a second plate`() {
        // The badge hangs off the well's bottom-right corner (AddGame), so it
        // must stay small relative to the piece it sits beside, and big enough
        // to read from across a room.
        for (objectSize in listOf(40.dp, 47.6.dp, 59.5.dp, 80.dp, AddCap, SingleCap)) {
            val badge = badgeDiameter(objectSize)
            assertTrue("badge $badge too small on $objectSize", badge >= 34.dp)
            assertTrue("badge $badge too big on $objectSize", badge <= 40.dp)
            assertTrue("badge $badge wider than its piece $objectSize", badge <= objectSize + 0.01.dp)
            assertTrue("badge overhang eats the plate gap", BadgeOverhang < PlateGap)
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
    fun `row plans are full rows plus a centred remainder`() {
        // The deterministic tray layout chunks by [rowPlan], so the plan
        // itself carries the promise: full rows of exactly perRow, a final
        // remainder that is never a lonely one (for the template's rows).
        for (count in 1..10) {
            val plan = rowPlan(count, perRowTemplate(count))
            assertEquals("count $count lost items", count, plan.sum())
            assertTrue(plan.all { it <= perRowTemplate(count) })
            assertTrue(plan.dropLast(1).all { it == perRowTemplate(count) })
            if (count >= 2) {
                assertTrue("count $count plans an orphan", plan.last() >= 2 || plan.size == 1)
            }
        }
        assertEquals(emptyList<Int>(), rowPlan(0, 4))
        assertEquals(listOf(3, 2), rowPlan(5, 3))
        assertEquals(listOf(1, 1, 1), rowPlan(3, 1))
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
