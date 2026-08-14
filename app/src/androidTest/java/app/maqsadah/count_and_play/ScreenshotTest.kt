package app.maqsadah.count_and_play

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.maqsadah.count_and_play.copy.BnCopy
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.EnCopy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.CountState
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.TakeState
import app.maqsadah.count_and_play.core.Token
import app.maqsadah.count_and_play.host.Flash
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.host.UiModel
import app.maqsadah.count_and_play.ui.GameScreen
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped UI, for the Play Store listing.
 *
 * These are ordinary state renders, not a live playthrough: every screen below
 * is a [UiModel] handed straight to [GameScreen]. That is only possible
 * because no composable in this app takes a ViewModel, so there is no TTS, no
 * coroutine timing and no emulator flakiness involved in capturing them, and
 * states that are awkward to reach by playing are trivial to photograph.
 *
 * The PNGs are written to the directory the instrumentation reports as
 * additional test output; the screenshots workflow
 * (.github/workflows/screenshots.yml) pulls them off the emulator and prefixes
 * each with the form factor.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir: File by lazy {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        File(path).apply { mkdirs() }
    }

    /**
     * The rule permits exactly one `setContent` per test, so the content is
     * mounted once and every scene is a state change pushed into it, which is
     * also the faster way round, and only possible because the whole screen is
     * a function of one immutable state.
     */
    private val scene = mutableStateOf(model(Screen.Home))

    @Test
    fun captureStoreScreenshots() {
        compose.setContent {
            // No theme wrapper: GameScreen paints its own ground.
            GameScreen(
                ui = scene.value,
                onChoose = {},
                onTapToken = {},
                onPour = {},
                onHome = {},
                onOpenSettings = {},
                onCloseSettings = {},
                onSetLanguage = {},
                onToggleMute = {},
            )
        }

        // The shelf: everything the app offers, on the very first screen.
        shoot("01_home") { model(Screen.Home) }

        // Mid-count: two of four tagged, so the number chips 1 and 2 show.
        shoot("02_count") {
            model(Screen.Count(CountState(tokens = tray(ShapeKind.APPLE, n = 4, counted = 2))))
        }

        // Both plates fully counted, the pour button awake.
        shoot("03_add") { model(Screen.Add(addReady())) }

        // The whole, with the parts still visible inside it: 3 + 2 = 5.
        shoot("04_add_fact") {
            model(Screen.Add(addPoured()), flash = Flash.Add(3, 2, 5))
        }

        // Mid-take: two of five gone, their ghosts on the tray; the child
        // is about to count what is left.
        shoot("05_take") {
            model(Screen.Take(TakeState(n = 5, b = 2, tokens = bowlOfBalls(gone = 2))))
        }

        // The take-away fact, after counting the leftovers: 5 - 2 = 3.
        shoot("06_take_fact") {
            model(
                Screen.Take(TakeState(n = 5, b = 2, tokens = takeCounted())),
                flash = Flash.Take(5, 2, 3),
            )
        }

        // The grown-up corner, open over the shelf.
        shoot("07_settings") { model(Screen.Home, settingsOpen = true) }

        // The same counting moment, in Bengali.
        shoot("08_bangla") {
            model(
                Screen.Count(CountState(tokens = tray(ShapeKind.MELON, n = 4, counted = 1))),
                copy = BnCopy,
            )
        }
    }

    // -- Fixtures -----------------------------------------------------------

    /** Every fixture keeps confettiKey = 0, so no burst fires mid-capture. */
    private fun model(
        screen: Screen,
        copy: Copy = EnCopy,
        settingsOpen: Boolean = false,
        flash: Flash? = null,
    ) = UiModel(
        screen = screen,
        copy = copy,
        muted = false,
        settingsOpen = settingsOpen,
        firstRun = false,
        flash = flash,
        confettiKey = 0,
    )

    /** A COUNT tray of [n] tokens, the first [counted] of them already tagged. */
    private fun tray(shape: ShapeKind, n: Int, counted: Int): List<Token> =
        List(n) { i ->
            Token(id = i + 1, shape = shape, counted = i < counted, countOrder = if (i < counted) i + 1 else 0)
        }

    /** 3 + 2, both plates fully counted and the button awake. */
    private fun addReady() = AddState(
        a = 3, b = 2,
        plateA = listOf(
            Token(id = 1, shape = ShapeKind.APPLE, counted = true, countOrder = 1),
            Token(id = 2, shape = ShapeKind.APPLE, counted = true, countOrder = 2),
            Token(id = 3, shape = ShapeKind.APPLE, counted = true, countOrder = 3),
        ),
        plateB = listOf(
            Token(id = 4, shape = ShapeKind.CARROT, counted = true, countOrder = 1),
            Token(id = 5, shape = ShapeKind.CARROT, counted = true, countOrder = 2),
        ),
    )

    /** The same 3 + 2 poured and the bowl fully counted. */
    private fun addPoured() = AddState(
        a = 3, b = 2,
        plateA = emptyList(),
        plateB = emptyList(),
        poured = true,
        bowl = listOf(
            Token(id = 1, shape = ShapeKind.APPLE, counted = true, countOrder = 1, origin = 1),
            Token(id = 2, shape = ShapeKind.APPLE, counted = true, countOrder = 2, origin = 1),
            Token(id = 3, shape = ShapeKind.APPLE, counted = true, countOrder = 3, origin = 1),
            Token(id = 4, shape = ShapeKind.CARROT, counted = true, countOrder = 4, origin = 2),
            Token(id = 5, shape = ShapeKind.CARROT, counted = true, countOrder = 5, origin = 2),
        ),
    )

    /** A TAKE bowl of five balls, the first [gone] of them removed. */
    private fun bowlOfBalls(gone: Int): List<Token> =
        List(5) { i ->
            Token(id = i + 1, shape = ShapeKind.BALL,
                gone = i < gone, countOrder = if (i < gone) i + 1 else 0)
        }

    /** 5 - 2 with the leftovers counted: two gone wearing their take-away
     *  numbers, the three left counted 1..3. */
    private fun takeCounted(): List<Token> =
        listOf(
            Token(id = 1, shape = ShapeKind.BALL, gone = true, countOrder = 1),
            Token(id = 2, shape = ShapeKind.BALL, gone = true, countOrder = 2),
            Token(id = 3, shape = ShapeKind.BALL, counted = true, countOrder = 1),
            Token(id = 4, shape = ShapeKind.BALL, counted = true, countOrder = 2),
            Token(id = 5, shape = ShapeKind.BALL, counted = true, countOrder = 3),
        )

    // -- Capture ------------------------------------------------------------

    private fun shoot(name: String, state: () -> UiModel) {
        compose.runOnUiThread { scene.value = state() }
        compose.waitForIdle()
        // The flash card pops in on a bouncy spring; give it a beat to land.
        Thread.sleep(SETTLE_MS)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private companion object {
        const val SETTLE_MS = 600L
    }
}
