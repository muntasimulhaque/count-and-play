package app.maqsadah.count_and_play

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped UI, for the Play Store listing.
 *
 * These are ordinary state renders, not a live playthrough: every screen below
 * is a [UiModel] handed straight to [GameScreen] inside a bare activity. That
 * is only possible because no composable in this app takes a ViewModel, so
 * there is no TTS, no coroutine timing and no emulator flakiness involved in
 * capturing them, and states that are awkward to reach by playing are trivial
 * to photograph.
 *
 * The harness deliberately avoids the compose test rule and everything under
 * it: no touch injection and no semantics queries are needed to render and
 * copy pixels, and dropping that machinery keeps these captures working on
 * whatever framework image the app targets, forever.
 *
 * The PNGs are written to the directory the instrumentation reports as
 * additional test output; the screenshots workflow
 * (.github/workflows/screenshots.yml) pulls them off the emulator and prefixes
 * each with the form factor.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private fun resolveOutDir(): File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (path != null) {
            val dir = File(path)
            if (dir.isDirectory || dir.mkdirs()) return dir
            // Cold-booted emulators can lag mounting shared storage; fall
            // back rather than fail.
        }
        return File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        ).apply { mkdirs() }
    }

    /**
     * One activity hosts every scene: each is a state change pushed into it,
     * which is also the faster way round, and only possible because the whole
     * screen is a function of one immutable state.
     */
    private val scene = mutableStateOf(model(Screen.Home))

    private fun launch(): ActivityScenario<ComponentActivity> {
        // Right after a cold boot the package manager can briefly refuse to
        // resolve; a short retry absorbs it without masking real breakage.
        var lastError: RuntimeException? = null
        repeat(3) { attempt ->
            try {
                val scenario = ActivityScenario.launch(ComponentActivity::class.java)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    activity.setContent { GameScreen(ui = scene.value, onChoose = {}, onTapToken = {}, onPour = {}, onHome = {}, onOpenSettings = {}, onCloseSettings = {}, onSetLanguage = {}, onToggleMute = {}) }
                }
                settle()
                return scenario
            } catch (e: RuntimeException) {
                lastError = e
                Thread.sleep(5000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("could not launch the host activity")
    }

    private fun push(state: UiModel) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { scene.value = state }
        settle()
    }

    /** Drain the main thread, then give animations a beat to land. */
    private fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(SETTLE_MS)
    }

    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()

        // The shelf: everything the app offers, on the very first screen.
        shoot(outDir, scenario, "01_home") { model(Screen.Home) }

        // Mid-count: two of four tagged, so the number chips 1 and 2 show.
        shoot(outDir, scenario, "02_count") {
            model(Screen.Count(CountState(tokens = tray(ShapeKind.APPLE, n = 4, counted = 2))))
        }

        // Both plates fully counted, the pour button awake.
        shoot(outDir, scenario, "03_add") { model(Screen.Add(addReady())) }

        // The whole, with the parts still visible inside it: 3 + 2 = 5.
        shoot(outDir, scenario, "04_add_fact") {
            model(Screen.Add(addPoured()), flash = Flash.Add(3, 2, 5))
        }

        // Mid-take: two of five gone, their ghosts on the tray; the child
        // is about to count what is left.
        shoot(outDir, scenario, "05_take") {
            model(Screen.Take(TakeState(n = 5, b = 2, tokens = bowlOfBalls(gone = 2))))
        }

        // The take-away fact, after counting the leftovers: 5 - 2 = 3.
        shoot(outDir, scenario, "06_take_fact") {
            model(
                Screen.Take(TakeState(n = 5, b = 2, tokens = takeCounted())),
                flash = Flash.Take(5, 2, 3),
            )
        }

        // The grown-up corner, open over the shelf.
        shoot(outDir, scenario, "07_settings") { model(Screen.Home, settingsOpen = true) }

        // The same counting moment, in Bengali.
        shoot(outDir, scenario, "08_bangla") {
            model(
                Screen.Count(CountState(tokens = tray(ShapeKind.MELON, n = 4, counted = 1))),
                copy = BnCopy,
            )
        }

        scenario.close()
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
    private fun tray(shape: ShapeKind, n: Int, counted: Int): PersistentList<Token> =
        List(n) { i ->
            Token(id = i + 1, shape = shape, counted = i < counted, countOrder = if (i < counted) i + 1 else 0)
        }.toPersistentList()

    /** A plate of [count] [shape]s, the first [counted] of them tagged. */
    private fun plate(count: Int, shape: ShapeKind, counted: Int, firstId: Int, origin: Int = 0) =
        List(count) { i ->
            Token(
                id = firstId + i,
                shape = shape,
                counted = i < counted,
                countOrder = if (i < counted) i + 1 else 0,
                origin = origin,
            )
        }.toPersistentList()

    /** 3 + 2, both plates fully counted (their totals worn) and the button awake. */
    private fun addReady() = AddState(
        a = 3, b = 2,
        plateA = plate(3, ShapeKind.APPLE, counted = 3, firstId = 1),
        plateB = plate(2, ShapeKind.CARROT, counted = 2, firstId = 4),
        doneA = true,
        doneB = true,
    )

    /** 5 + 5 poured: ten in the bowl, [counted] of them tagged afresh; the
     *  emptied plates keep their totals. */
    private fun addPouredBig(counted: Int) = AddState(
        a = 5, b = 5,
        plateA = persistentListOf(),
        plateB = persistentListOf(),
        poured = true,
        doneA = true,
        doneB = true,
        bowl = (
            plate(5, ShapeKind.APPLE, counted = counted.coerceAtMost(5), firstId = 1, origin = 1) +
                plate(5, ShapeKind.CARROT, counted = (counted - 5).coerceAtLeast(0), firstId = 6, origin = 2)
            ).toPersistentList(),
    )

    /** The same 3 + 2 poured and the bowl fully counted. */
    private fun addPoured() = addPouredBig(counted = 5)

    /** A TAKE bowl of five balls, the first [gone] of them removed. */
    private fun bowlOfBalls(gone: Int): PersistentList<Token> =
        List(5) { i ->
            Token(id = i + 1, shape = ShapeKind.BALL,
                gone = i < gone, countOrder = if (i < gone) i + 1 else 0)
        }.toPersistentList()

    /** 5 - 2 with the leftovers counted: two gone wearing their take-away
     *  numbers, the three left counted 1..3. */
    private fun takeCounted(): PersistentList<Token> =
        listOf(
            Token(id = 1, shape = ShapeKind.BALL, gone = true, countOrder = 1),
            Token(id = 2, shape = ShapeKind.BALL, gone = true, countOrder = 2),
            Token(id = 3, shape = ShapeKind.BALL, counted = true, countOrder = 1),
            Token(id = 4, shape = ShapeKind.BALL, counted = true, countOrder = 2),
            Token(id = 5, shape = ShapeKind.BALL, counted = true, countOrder = 3),
        ).toPersistentList()

    // -- Capture ------------------------------------------------------------

    private fun shoot(outDir: File, scenario: ActivityScenario<ComponentActivity>, name: String, state: () -> UiModel) {
        push(state())
        lateinit var bitmap: Bitmap
        scenario.onActivity { activity -> bitmap = captureWindow(activity) }
        File(outDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /** The activity's own window pixels: the truth the child actually sees. */
    private fun captureWindow(activity: ComponentActivity): Bitmap {
        val decor = activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        PixelCopy.request(activity.window, bitmap, { result ->
            if (result != PixelCopy.SUCCESS) {
                // Software draw as the fallback path; static candy renders fine.
                decor.draw(android.graphics.Canvas(bitmap))
            }
            latch.countDown()
        }, Handler(Looper.getMainLooper()))
        latch.await(10, TimeUnit.SECONDS)
        return bitmap
    }

    private companion object {
        const val SETTLE_MS = 600L
    }
}
