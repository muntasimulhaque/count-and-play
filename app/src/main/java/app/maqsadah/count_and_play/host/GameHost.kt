package app.maqsadah.count_and_play.host

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.copy.copyFor
import app.maqsadah.count_and_play.core.Beat
import app.maqsadah.count_and_play.core.Event
import app.maqsadah.count_and_play.core.Lesson
import app.maqsadah.count_and_play.core.LessonState
import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.Scheduler
import app.maqsadah.count_and_play.core.Script
import app.maqsadah.count_and_play.core.SeededRng
import app.maqsadah.count_and_play.core.SessionState
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.StageChange
import app.maqsadah.count_and_play.core.Step
import app.maqsadah.count_and_play.core.TaskResult
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.data.Settings
import app.maqsadah.count_and_play.data.SettingsStore
import app.maqsadah.count_and_play.sound.SoundBoard
import app.maqsadah.count_and_play.speech.Narrator
import app.maqsadah.count_and_play.speech.SilentNarrator
import app.maqsadah.count_and_play.speech.TtsNarrator
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coordination, and nothing else.
 *
 * The rules are in `core`, the words are in `copy`, the timing is in
 * [ScriptRunner]. What is left is small: hold the session, turn a tap into an
 * event, and play what the domain hands back.
 */
class GameHost(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)
    private val sounds = SoundBoard(application)
    private val rng = SeededRng(System.nanoTime())

    private var tts: TtsNarrator? = null
    private val silent = SilentNarrator()
    private var foreground = true

    private var session: SessionState
    private var lesson: LessonState? = null
    private var scriptJob: Job? = null
    private var idleJob: Job? = null

    var ui by mutableStateOf(GameUiState())
        private set

    private val runner = ScriptRunner(
        narrator = ::activeNarrator,
        timing = Timing.Real,
        onSound = sounds::play,
        onChange = ::applyChange,
        render = { words.speak(it) },
    )

    init {
        val settings = store.load()
        val progress = store.loadProgress()
        session = SessionState(progress = progress, shape = settings.shape)
        sounds.enabled = settings.soundOn
        ui = GameUiState(
            screen = if (settings.languageChosen) Screen.SHAPE else Screen.LANGUAGE,
            settings = settings,
            progress = progress,
            tasksInSession = Scheduler.TASKS_PER_SESSION,
        )
        if (settings.languageChosen) startSpeech(settings)
    }

    private val words: Copy get() = copyFor(ui.settings.language)

    private fun activeNarrator(): Narrator =
        // Muting swaps the narrator, not the pacing. The old build reused the
        // speech path's 350 ms stub, so muting fast-forwarded the whole game
        // and a ten-second phase collapsed into two.
        if (ui.settings.soundOn && foreground) tts ?: silent else silent

    // -- Choices the child and the grown-up make ----------------------------

    fun chooseLanguage(language: Language) {
        val settings = ui.settings.copy(language = language, languageChosen = true)
        store.save(settings)
        ui = ui.copy(settings = settings, screen = Screen.SHAPE)
        startSpeech(settings)
    }

    /** The app's opening act is a real choice, and the most natural one there
     *  is: what shall we count? The old build handed it to a random number. */
    fun chooseShape(shape: ShapeKind) {
        val settings = ui.settings.copy(shape = shape)
        store.save(settings)
        session = session.copy(shape = shape)
        ui = ui.copy(settings = settings)
        next()
    }

    fun tapToken(id: Int) = handle(Event.TapToken(id))

    fun tapZone(zone: Zone) = handle(Event.TapZone(zone))

    fun done() = handle(Event.Done)

    /** Nothing advances on its own. The child decides when to move on. */
    fun next() {
        cancelIdle()
        if (session.isComplete) return endSession()

        val task = Scheduler.task(session, rng)
        val outcome = Lesson.begin(task, session.progress)
        lesson = outcome.state
        ui = ui.copy(
            screen = Screen.PLAY,
            lesson = outcome.state,
            fx = Fx(),
            taskIndex = session.index,
        )
        play(outcome.script)
    }

    /** Leaves the round without losing anything already recorded. */
    fun leaveSession() {
        cancelIdle()
        scriptJob?.cancel()
        tts?.stopNow()
        lesson = null
        ui = ui.copy(screen = Screen.SHAPE, lesson = null, fx = Fx())
    }

    fun playAgain() {
        session = SessionState(progress = Scheduler.close(session), shape = ui.settings.shape)
        store.saveProgress(session.progress)
        ui = ui.copy(screen = Screen.SHAPE, progress = session.progress, taskIndex = 0, lesson = null)
    }

    private fun endSession() {
        val progress = Scheduler.close(session)
        session = session.copy(progress = progress)
        store.saveProgress(progress)
        lesson = null
        ui = ui.copy(screen = Screen.DONE, progress = progress, lesson = null)
        play(app.maqsadah.count_and_play.core.script { say(app.maqsadah.count_and_play.core.Line.SessionDone) })
    }

    // -- The loop -----------------------------------------------------------

    private fun handle(event: Event) {
        val current = lesson ?: return
        val outcome = Lesson.onEvent(current, event)

        // A tap that moved nothing — a dead slot, a re-tap — is still
        // acknowledged with its sound, but it must NOT cancel narration that is
        // mid-sentence. Otherwise a child drumming on the tray silences the
        // very moment the app names the whole set.
        val moved = outcome.state.tokens != current.tokens ||
            outcome.state.step != current.step ||
            outcome.result != null

        lesson = outcome.state
        ui = ui.copy(lesson = outcome.state)

        if (!moved) {
            outcome.script.beats
                .filterIsInstance<Beat.Cue>()
                .forEach { sounds.play(it.sound) }
            return
        }

        cancelIdle()
        outcome.result?.let(::record)
        play(outcome.script)
    }

    private fun record(result: TaskResult) {
        session = Scheduler.record(session, result)
        store.saveProgress(session.progress)
        ui = ui.copy(progress = session.progress, taskIndex = session.index)
    }

    /**
     * Cancels whatever is speaking and plays the new script.
     *
     * `cancelAndJoin`, never a bare `cancel()`: a bare cancel returns before
     * cleanup runs, and that window is exactly where two voices overlap.
     */
    private fun play(script: Script) {
        val previous = scriptJob
        scriptJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            runner.play(script)
            armIdle()
        }
    }

    private fun applyChange(change: StageChange) {
        val fx = ui.fx
        ui = ui.copy(
            fx = when (change) {
                is StageChange.Drop -> fx.copy(revealed = fx.revealed + change.ids, highlighted = emptySet())
                is StageChange.Travel -> fx.copy(revealed = fx.revealed + change.ids, highlighted = emptySet())
                is StageChange.Collapse -> fx.copy(cardinals = fx.cardinals + (change.zone to change.total))
                is StageChange.Compact -> fx
                is StageChange.Cover -> fx.copy(covered = fx.covered + change.zone)
                is StageChange.Uncover -> fx.copy(covered = fx.covered - change.zone)
                is StageChange.Highlight -> fx.copy(highlighted = change.ids.toSet())
                StageChange.ShowPrediction -> fx.copy(predicting = true)
                StageChange.Celebrate -> fx.copy(celebrating = true)
            },
        )
    }

    // -- The nudge ladder ---------------------------------------------------

    private fun armIdle() {
        cancelIdle()
        val state = lesson ?: return
        if (state.step == Step.Finished) return
        val wait = idleDelay(state.nudges) ?: return

        idleJob = viewModelScope.launch {
            delay(wait)
            val outcome = Lesson.onEvent(state, Event.Nudge)
            lesson = outcome.state
            ui = ui.copy(lesson = outcome.state)
            // An empty script means the ladder has run out. It then stops for
            // good rather than calling out every few seconds forever.
            if (!outcome.script.isEmpty) play(outcome.script) else armIdle()
        }
    }

    /** Well past a 3-year-old's normal thinking time, and finite. */
    private fun idleDelay(nudges: Int): Long? = when (nudges) {
        0 -> 8_000L
        1 -> 10_000L
        2 -> 16_000L
        else -> null
    }

    private fun cancelIdle() {
        idleJob?.cancel()
        idleJob = null
    }

    // -- Grown-ups ----------------------------------------------------------

    fun openSettings() {
        cancelIdle()
        ui = ui.copy(settingsOpen = true, confirmingReset = false)
    }

    fun closeSettings() {
        ui = ui.copy(settingsOpen = false, confirmingReset = false)
        armIdle()
    }

    fun setSound(on: Boolean) {
        val settings = ui.settings.copy(soundOn = on)
        store.save(settings)
        sounds.enabled = on
        if (!on) tts?.stopNow()
        ui = ui.copy(settings = settings)
    }

    fun setSlowRate(slow: Boolean) {
        val settings = ui.settings.copy(slowRate = slow)
        store.save(settings)
        tts?.slowRate = slow
        ui = ui.copy(settings = settings)
    }

    fun setLanguage(language: Language) {
        val settings = ui.settings.copy(language = language, languageChosen = true)
        store.save(settings)
        ui = ui.copy(settings = settings)
        // Relabelling is enough — the domain emits typed lines, not strings, so
        // a language change mid-round changes only how they are worded.
        startSpeech(settings)
    }

    fun askReset() {
        ui = ui.copy(confirmingReset = true)
    }

    fun confirmReset() {
        store.resetProgress()
        session = SessionState(progress = Progress(), shape = ui.settings.shape)
        lesson = null
        ui = ui.copy(
            progress = Progress(),
            confirmingReset = false,
            settingsOpen = false,
            screen = Screen.SHAPE,
            lesson = null,
            taskIndex = 0,
        )
    }

    // -- Lifecycle ----------------------------------------------------------

    private fun startSpeech(settings: Settings) {
        tts?.release()
        val narrator = TtsNarrator(getApplication(), settings.language)
        narrator.slowRate = settings.slowRate
        narrator.useVoice(store.voiceName(settings.language))
        tts = narrator
        ui = ui.copy(voiceMissing = !narrator.voiceAvailable)
    }

    /** Backgrounding stops the app dead, rather than leaving it talking and
     *  playing on unseen. State survives; the round picks up where it stopped. */
    fun onEnterBackground() {
        foreground = false
        cancelIdle()
        scriptJob?.cancel()
        tts?.stopNow()
    }

    fun onReturnToForeground() {
        foreground = true
        val state = lesson ?: return
        // He comes back and is told again what to do, rather than to silence.
        play(Lesson.reprompt(state))
    }

    override fun onCleared() {
        scriptJob?.cancel()
        idleJob?.cancel()
        tts?.release()
        sounds.release()
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameHost(application) as T
        }
    }
}
