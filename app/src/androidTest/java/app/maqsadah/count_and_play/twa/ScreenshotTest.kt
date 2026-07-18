package app.maqsadah.count_and_play.twa

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.MainScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Captures REAL store screenshots straight from the app's Compose UI on an
 * emulator (so emoji, fonts, spacing and the auto-fit sizing are exactly what a
 * user sees — unlike the old PIL-rendered approximations).
 *
 * Each scene is staged by setting the [GameController]'s public state directly
 * instead of driving the UI, so captures are deterministic and never flaky. The
 * PNGs land in the app's external files dir; CI pulls them with `adb pull`.
 *
 * Run on a device/emulator via `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val speaker = Speaker(context)

    private fun controller() = GameController(speaker, MainScope())

    /** A settled item: drop-in animation already done, no count bubble. */
    private fun item(id: Int, emoji: String, group: Char, ghost: Boolean = false) =
        Item(id, emoji, group).apply {
            dropped = true
            this.ghost = ghost
        }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        // Internal files dir (/data/data/<pkg>/files/screenshots). CI pulls these
        // with `run-as` — reliable on Android 14, where /sdcard/Android/data is
        // blocked to adb. targetContext is the app under test, so run-as matches.
        val dir = File(context.filesDir, "screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use {
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

    @Test
    fun b_learnAdd() {
        val c = controller()
        c.mode = Mode.LEARN
        c.screen = Screen.GAME
        c.pickerVisible = false
        // Same-object addition (13 stars + 7 stars), the v2.3 behaviour.
        c.equation = listOf(EqPart("13 + 7 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 13
        c.capB = 7
        c.plateBVisible = true
        var id = 1
        repeat(13) { c.plateA.add(item(id++, "⭐", 'A')) }   // ⭐
        repeat(7) { c.plateB.add(item(id++, "⭐", 'B')) }    // ⭐
        c.prompt = "Tap to put them together!"
        c.showMerge = true
        rule.setContent { CountPlayApp(c, speaker) }
        capture("02_learn_add")
    }

    @Test
    fun c_quiz() {
        val c = controller()
        c.mode = Mode.QUIZ
        c.screen = Screen.GAME
        c.equation = listOf(EqPart("3 + 2 = ", EqPart.NORMAL), EqPart("?", EqPart.Q))
        c.capA = 5
        var id = 1
        repeat(5) { c.plateA.add(item(id++, "🍎", 'A')) }  // 🍎
        c.prompt = "How many altogether?"
        c.answers.addAll(listOf(4, 5, 6))
        rule.setContent { CountPlayApp(c, speaker) }
        capture("03_quiz")
    }

    @Test
    fun d_learnSub() {
        val c = controller()
        c.mode = Mode.LEARN
        c.screen = Screen.GAME
        c.equation = listOf(EqPart("8 − 3 = ", EqPart.NORMAL), EqPart("?", EqPart.Q)) // 8 − 3
        c.capA = 8
        var id = 1
        repeat(5) { c.plateA.add(item(id++, "🍌", 'A')) }  // 🍌 (5 left)
        c.goneVisible = true
        c.goneLabel = "take away 3"
        c.capGone = 3
        repeat(3) { c.slots.add(item(id++, "🍌", 'A', ghost = true)) } // 3 taken
        c.prompt = "How many are left?"
        rule.setContent { CountPlayApp(c, speaker) }
        capture("04_learn_sub")
    }
}
