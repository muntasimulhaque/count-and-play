package app.maqsadah.count_and_play.host

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.copy.copyOf
import app.maqsadah.count_and_play.core.Adapt
import app.maqsadah.count_and_play.core.Beat
import app.maqsadah.count_and_play.core.Round
import app.maqsadah.count_and_play.core.SeededRng
import app.maqsadah.count_and_play.core.SessionState
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.core.choose as coreChoose
import app.maqsadah.count_and_play.core.deal
import app.maqsadah.count_and_play.data.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordination, and nothing else. The rules live in `core`, the words in
 * `copy`; the host owns pacing (one beat performance at a time), persistence,
 * and the two effect players.
 */
class GameHost(application: Application) : AndroidViewModel(application) {

    private val store = Store(application)
    private val sounds = SoundBoard(application)
    val narrator = Narrator(application, store.language ?: Language.EN)
    private val rng = SeededRng(System.currentTimeMillis())

    private var session: SessionState? = null
    private var settingsOpen = false
    private var flash: Flash? = null
    private var confettiKey = 0
    private var performance: Job? = null

    // Levels survive restarts; streaks do not (a streak is a run, not a fact).
    private var adaptCount = Adapt(level = store.levelCount)
    private var adaptAdd = Adapt(level = store.levelAdd)
    private var adaptTake = Adapt(level = store.levelTake)

    private val _ui = MutableStateFlow(model())
    val ui: StateFlow<UiModel> = _ui.asStateFlow()

    private val language: Language get() = store.language ?: Language.EN
    private val copy: Copy get() = copyOf(language)

    init {
        narrator.setMuted(store.muted)
    }

    // -- The child's choices -------------------------------------------------

    fun choose(skill: Skill) {
        hush()
        flash = null
        val seeded = coreChoose(skill, rng).copy(
            adaptCount = adaptCount,
            adaptAdd = adaptAdd,
            adaptTake = adaptTake,
        )
        // Deal at the stored level, so a returning child never regresses to
        // level 0 for one round after a restart.
        val next = seeded.copy(round = deal(skill, seeded.adapt(skill).level, rng))
        session = next
        publish()
        perform(next.round.startBeats())
    }

    fun tap(id: Int) {
        val current = session ?: return
        // Taps during the celebration dwell must not count as struggles.
        if (current.round.done) return
        val (next, beats) = current.tap(id)
        session = next
        publish()
        perform(beats)
    }

    /** The ADD pour button: the one in-round action that is not a token tap. */
    fun pour() {
        val current = session ?: return
        if (current.round.done) return
        val (next, beats) = current.pour()
        session = next
        publish()
        perform(beats)
    }

    fun home() {
        hush()
        session = null
        flash = null
        publish()
    }

    // -- Grown-ups -------------------------------------------------------------

    fun openSettings() {
        settingsOpen = true
        publish()
    }

    fun closeSettings() {
        settingsOpen = false
        publish()
    }

    fun setLanguage(language: Language) {
        store.language = language
        store.languageChosen = true
        narrator.setLanguage(language)
        // Relabel only: beats carry typed lines, so the new Copy rewords
        // whatever is said next. Nothing is retold.
        publish()
    }

    fun toggleMute() {
        store.muted = !store.muted
        narrator.setMuted(store.muted)
        publish()
    }

    // -- Lifecycle -------------------------------------------------------------

    /** Backgrounding stops the app dead, rather than leaving it talking unseen. */
    fun pause() {
        performance?.cancel()
        narrator.pause()
    }

    fun resume() = narrator.resume()

    override fun onCleared() {
        performance?.cancel()
        narrator.release()
        sounds.release()
    }

    // -- Beat performance ------------------------------------------------------

    /** One performance at a time: starting a new one cancels and joins the
     *  previous, so two scripts can never speak over each other. */
    private fun perform(beats: List<Beat>) {
        val previous = performance
        performance = viewModelScope.launch {
            previous?.cancelAndJoin()
            for (beat in beats) render(beat)
            val current = session ?: return@launch
            if (current.round.done) {
                delay(2300L) // the big-numeral moment stays on screen
                advance()
            }
        }
    }

    private suspend fun render(beat: Beat) {
        when (beat) {
            is Beat.Play -> {
                sounds.play(beat.sfx)
                delay(150L)
            }
            is Beat.SayCount -> say(copy.numberWord(beat.n))
            is Beat.SayCardinal -> say(copy.cardinal(beat.n))
            is Beat.SayPromptCount -> say(copy.promptCount())
            is Beat.SayPromptAdd -> say(copy.promptAdd())
            is Beat.SayPromptAll -> say(copy.promptAll())
            is Beat.SayPromptTake -> say(copy.promptTake(beat.b))
            is Beat.SayPromptLeft -> say(copy.promptLeft())
            is Beat.SayFactAdd -> say(copy.factAdd(beat.a, beat.b, beat.total))
            is Beat.SayFactTake -> say(copy.factTake(beat.n, beat.b, beat.left))
            is Beat.SayCelebrate -> say(copy.celebrate())
            is Beat.FlashCount -> {
                flash = Flash.Count(beat.n)
                publish()
            }
            is Beat.FlashAdd -> {
                flash = Flash.Add(beat.a, beat.b, beat.total)
                publish()
            }
            is Beat.FlashTake -> {
                flash = Flash.Take(beat.n, beat.b, beat.left)
                publish()
            }
            Beat.Confetti -> {
                confettiKey++
                publish()
            }
        }
    }

    private suspend fun say(line: String) {
        narrator.speak(line)
        delay(700L)
    }

    /** Records the finished round into its Adapt, persists the levels, and
     *  deals the next round of the same skill. */
    private suspend fun advance() {
        val current = session ?: return
        if (!current.round.done) return
        val (next, startBeats) = current.nextRound()
        session = next
        store.levelCount = next.adaptCount.level
        store.levelAdd = next.adaptAdd.level
        store.levelTake = next.adaptTake.level
        adaptCount = next.adaptCount
        adaptAdd = next.adaptAdd
        adaptTake = next.adaptTake
        flash = null
        publish()
        for (beat in startBeats) render(beat)
    }

    /** Stops whatever is being said, without touching what is on screen. */
    private fun hush() {
        performance?.cancel()
        narrator.stop()
    }

    // -- UiModel ----------------------------------------------------------------

    private fun publish() {
        _ui.value = model()
    }

    private fun model(): UiModel {
        val round = session?.round
        return UiModel(
            screen = when (round) {
                null -> Screen.Home
                is Round.IsCount -> Screen.Count(round.state)
                is Round.IsAdd -> Screen.Add(round.state)
                is Round.IsTake -> Screen.Take(round.state)
            },
            copy = copy,
            muted = store.muted,
            settingsOpen = settingsOpen,
            firstRun = !store.languageChosen,
            flash = flash,
            confettiKey = confettiKey,
        )
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameHost(application) as T
        }
    }
}
