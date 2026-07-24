package app.maqsadah.count_and_play

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Captures REAL store screenshots straight from the app's Compose UI on an
 * emulator (so emoji, fonts, spacing and the rows-of-five auto-fit sizing are
 * exactly what a user sees).
 *
 * Each scene is staged by setting the [GameViewModel]'s public state directly
 * instead of driving the UI, so captures are deterministic and never flaky. The
 * PNGs land in the app's additional test output dir; CI reads them from the
 * build outputs.
 *
 * Run on a device/emulator via `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val speaker = Speaker(context)

    private fun controller() =
        GameViewModel(context.applicationContext as Application, speaker, Lang.EN, true)

    /**
     * Where to write the PNGs. AGP passes `additionalTestOutputDir` and copies
     * whatever we write there into `app/build/outputs/connected_android_test_
     * additional_output/…` BEFORE it uninstalls the app — so CI just reads them
     * off the build output, with no adb pull / root / scoped-storage games.
     * Falls back to the app's files dir when run outside that AGP flow.
     */
    private val outDir: File by lazy {
        val extra = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        (if (extra != null) File(extra) else File(context.filesDir, "screenshots"))
            .apply { mkdirs() }
    }

    /** A settled item: drop-in animation already done, no animations pending. */
    private fun item(
        id: Int,
        emoji: String,
        group: Char,
        ghost: Boolean = false,
        badge: Int? = null,
        tappable: Boolean = false
    ) = Item(id, emoji, group).apply {
        dropped = true
        this.ghost = ghost
        this.badge = badge
        this.tappable = tappable
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "$name.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun a_menu() {
        val c = controller()
        c.screen = Screen.MENU
        rule.setContent { CountPlayApp(c, speaker) }
        capture("01_menu")
    }

    /** Guided addition mid-count: group A counted (badges 1..3), group B tappable. */
    @Test
    fun b_guidedAdd() {
        val c = controller()
        c.mode = Mode.GUIDED
        c.screen = Screen.GAME
        c.level = 2
        c.starsInLevel = 3
        c.phase = Phase.COUNT_B
        c.equation = listOf(EqPart("3 + 2 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 3
        c.capB = 2
        c.plateBVisible = true
        var id = 1
        repeat(3) { i -> c.plateA.add(item(id++, "🍎", 'A', badge = i + 1)) }
        repeat(2) { c.plateB.add(item(id++, "🍎", 'B', tappable = true)) }
        c.prompt = "Tap and count!"
        rule.setContent { CountPlayApp(c, speaker) }
        capture("02_guided_add")
    }

    /** Guided subtraction: 3 ghost holes keep their grid slots, 5 remain to count. */
    @Test
    fun c_guidedSub() {
        val c = controller()
        c.mode = Mode.GUIDED
        c.screen = Screen.GAME
        c.level = 3
        c.starsInLevel = 1
        c.phase = Phase.COUNT_LEFT
        c.equation = listOf(EqPart("8 − 3 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 8
        var id = 1
        // first five remain: two already tap-counted (badges), the rest tappable
        repeat(5) { i ->
            c.plateA.add(
                item(id++, "🍌", 'A', badge = if (i < 2) i + 1 else null, tappable = i >= 2)
            )
        }
        // the three take-aways are ghost holes in their original slots
        repeat(3) { c.plateA.add(item(id++, "🍌", 'A', ghost = true)) }
        c.prompt = "How many are left?"
        rule.setContent { CountPlayApp(c, speaker) }
        capture("03_guided_sub")
    }

    /** Quiz question: watch phase done, answer buttons up, objects tappable. */
    @Test
    fun d_quiz() {
        val c = controller()
        c.mode = Mode.QUIZ
        c.screen = Screen.GAME
        c.level = 2
        c.phase = Phase.ANSWER
        c.equation = listOf(EqPart("4 + 3 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 7
        var id = 1
        repeat(7) { i ->
            c.plateA.add(
                item(id++, "🍎", 'A', badge = if (i == 0) 1 else null, tappable = i > 0)
            )
        }
        c.prompt = "How many altogether?"
        c.answers.addAll(listOf(6, 7, 8))
        rule.setContent { CountPlayApp(c, speaker) }
        capture("04_quiz")
    }

    /** Level-up celebration over a finished round. */
    @Test
    fun e_levelUp() {
        val c = controller()
        c.mode = Mode.GUIDED
        c.screen = Screen.GAME
        c.level = 2
        c.starsInLevel = 0
        c.equation = listOf(EqPart("2 + 1 = 3", EqPart.NORMAL))
        c.capA = 3
        var id = 1
        repeat(3) { i -> c.plateA.add(item(id++, "⭐", 'A', badge = i + 1)) }
        c.prompt = "Two plus one makes three!"
        c.levelUpVisible = true
        rule.setContent { CountPlayApp(c, speaker) }
        capture("05_level_up")
    }

    /**
     * Regression: the reported "8 + 8" case that showed only 6 balls per box on a
     * Vivo Y90. Both boxes must render all 8 objects (rows of five: 5 + 3), no
     * clipped bottom row, at an identical size.
     */
    @Test
    fun f_add8plus8() {
        val c = controller()
        c.mode = Mode.FREE
        c.screen = Screen.GAME
        c.phase = Phase.MERGE
        c.equation = listOf(EqPart("8 + 8 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 8
        c.capB = 8
        c.plateBVisible = true
        var id = 1
        repeat(8) { i -> c.plateA.add(item(id++, "⚽", 'A', badge = i + 1)) }
        repeat(8) { i -> c.plateB.add(item(id++, "⚽", 'B', badge = i + 1)) }
        c.prompt = "Put them together!"
        c.showMerge = true
        rule.setContent { CountPlayApp(c, speaker) }
        capture("06_add_8plus8")
    }

    /**
     * Regression: the reported "1 + 14" case where the single star was huge and
     * the fourteen were small. Every star must be the same size in both boxes.
     */
    @Test
    fun g_add1plus14() {
        val c = controller()
        c.mode = Mode.FREE
        c.screen = Screen.GAME
        c.phase = Phase.MERGE
        c.equation = listOf(EqPart("1 + 14 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 1
        c.capB = 14
        c.plateBVisible = true
        var id = 1
        repeat(1) { i -> c.plateA.add(item(id++, "⭐", 'A', badge = i + 1)) }
        repeat(14) { i -> c.plateB.add(item(id++, "⭐", 'B', badge = i + 1)) }
        c.prompt = "Put them together!"
        c.showMerge = true
        rule.setContent { CountPlayApp(c, speaker) }
        capture("07_add_1plus14")
    }
}
