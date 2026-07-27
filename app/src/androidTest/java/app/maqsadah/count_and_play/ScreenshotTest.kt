package app.maqsadah.count_and_play

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Event
import app.maqsadah.count_and_play.core.Lesson
import app.maqsadah.count_and_play.core.LessonState
import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Step
import app.maqsadah.count_and_play.core.Task
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.core.inZone
import app.maqsadah.count_and_play.data.Settings
import app.maqsadah.count_and_play.host.Fx
import app.maqsadah.count_and_play.host.GameUiState
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.ui.CountPlayTheme
import app.maqsadah.count_and_play.ui.GameScreen
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped UI, for the Play Store listing.
 *
 * These are ordinary state renders, not a live playthrough: every screen below
 * is a `GameUiState` handed straight to `GameScreen`. That is only possible
 * because no composable in this app takes a ViewModel — so there is no TTS, no
 * coroutine timing and no emulator flakiness involved in capturing them, and
 * states that are awkward to reach by playing are trivial to photograph.
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
     * mounted once and every scene is a state change pushed into it — which is
     * also the faster way round, and only possible because the whole screen is
     * a function of one immutable state.
     */
    private val scene = mutableStateOf(GameUiState())

    @Test
    fun captureStoreScreenshots() {
        compose.setContent {
            CountPlayTheme(dark = false) {
                GameScreen(
                    state = scene.value,
                    onLanguage = {}, onShape = {}, onTapToken = {}, onTapZone = {},
                    onDone = {}, onNext = {}, onPlayAgain = {},
                    onOpenSettings = {}, onCloseSettings = {},
                    onSetSound = {}, onSetSlow = {}, onSetLanguage = {},
                    onAskReset = {}, onConfirmReset = {},
                )
            }
        }

        shoot("01_choose") { base(Screen.SHAPE) }

        shoot("02_counting") {
            val lesson = play(Task.CountIt(4, ShapeKind.APPLE), taps = 2)
            base(Screen.PLAY, lesson)
        }

        shoot("03_give_n") {
            val lesson = play(Task.GiveMe(3, 6, ShapeKind.STAR), moveToBowl = 2)
            base(Screen.PLAY, lesson)
        }

        shoot("04_which_is_more") {
            base(Screen.PLAY, play(Task.WhichIsMore(5, 3, false, ShapeKind.BEAD)))
        }

        // Addition, before the pour: two parts, each already a number.
        shoot("05_two_parts") {
            base(Screen.PLAY, play(Task.Join(3, 2, ShapeKind.CARROT), taps = 3))
        }

        // The whole, with the parts still visibly inside it.
        shoot("06_all_together") {
            val lesson = joinPoured(Task.Join(3, 2, ShapeKind.CARROT))
            base(Screen.PLAY, lesson)
        }

        // Take-away, after the removal has settled: what left is still there and
        // countable, and the remainder has compacted left so the trailing empty
        // cells read as "two fewer than five".
        shoot("07_taking_away") {
            val lesson = play(Task.Separate(5, 2, ShapeKind.BALL), taps = 5, takeOut = 2, then = true)
            base(Screen.PLAY, lesson)
        }

        shoot("08_settings") {
            base(Screen.PLAY, play(Task.CountIt(3, ShapeKind.TULIP), taps = 3))
                .copy(settingsOpen = true)
        }

        shoot("09_bangla") {
            val lesson = play(Task.CountIt(4, ShapeKind.MELON), taps = 4)
            base(Screen.PLAY, lesson).copy(
                settings = Settings(language = Language.BN, languageChosen = true),
            )
        }

        shoot("10_done") { base(Screen.DONE) }
    }

    // -- Fixtures -----------------------------------------------------------

    private fun play(
        task: Task,
        taps: Int = 0,
        moveToBowl: Int = 0,
        takeOut: Int = 0,
        /** Sends Done at the end, so the step settles rather than freezing mid-move. */
        then: Boolean = false,
    ): LessonState {
        var state = Lesson.begin(task, Progress()).state
        repeat(taps) {
            val next = (state.step as? Step.Counting)?.let { step ->
                state.tokens.inZone(step.zone).firstOrNull { t -> !t.isCounted }
            }
            next?.let { state = Lesson.onEvent(state, Event.TapToken(it.id)).state }
        }
        repeat(moveToBowl) {
            state.tokens.inZone(Zone.SOURCE).firstOrNull()
                ?.let { state = Lesson.onEvent(state, Event.TapToken(it.id)).state }
        }
        repeat(takeOut) {
            state.tokens.inZone(Zone.BOWL).firstOrNull()
                ?.let { state = Lesson.onEvent(state, Event.TapToken(it.id)).state }
        }
        if (then) state = Lesson.onEvent(state, Event.Done).state
        return state
    }

    /** Counts both dishes, predicts, and pours — the state addition exists for. */
    private fun joinPoured(task: Task.Join): LessonState {
        var state = Lesson.begin(task, Progress()).state
        while (state.step is Step.Counting || state.step == Step.Predicting) {
            val step = state.step
            val move = when {
                step is Step.Counting ->
                    state.tokens.inZone(step.zone).firstOrNull { !it.isCounted }
                        ?.let { Event.TapToken(it.id) } ?: Event.Done
                state.tokens.inZone(Zone.ANSWER).size < task.answer ->
                    Event.TapToken(state.tokens.inZone(Zone.RESERVE).first().id)
                else -> Event.Done
            }
            state = Lesson.onEvent(state, move).state
            if (state.tokens.inZone(Zone.BOWL).size == task.answer) break
        }
        return state
    }

    private fun base(screen: Screen, lesson: LessonState? = null) = GameUiState(
        screen = screen,
        settings = Settings(languageChosen = true),
        lesson = lesson,
        // Everything has landed: these are photographs, not animations.
        fx = Fx(
            revealed = lesson?.tokens?.map { it.id }?.toSet().orEmpty(),
            cardinals = cardinalsFor(lesson),
            predicting = lesson?.step == Step.Predicting,
        ),
    )

    private fun cardinalsFor(lesson: LessonState?): Map<Zone, Int> {
        if (lesson == null) return emptyMap()
        return Zone.entries.mapNotNull { zone ->
            val counted = lesson.tokens.inZone(zone).count { it.isCounted }
            val total = lesson.tokens.inZone(zone).size
            if (total > 0 && counted == total) zone to total else null
        }.toMap()
    }

    private fun shoot(name: String, state: () -> GameUiState) {
        compose.runOnUiThread { scene.value = state() }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
