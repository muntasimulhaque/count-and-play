package app.maqsadah.count_and_play.host

import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.AddState
import app.maqsadah.count_and_play.core.CountState
import app.maqsadah.count_and_play.core.TakeState

/** What is on screen. Compose renders this; the ViewModel produces it. */
sealed class Screen {
    data object Home : Screen()
    data class Count(val state: CountState) : Screen()
    data class Add(val state: AddState) : Screen()
    data class Take(val state: TakeState) : Screen()
}

/** The big numeral moment after a round completes. */
sealed class Flash {
    data class Count(val n: Int) : Flash()
    data class Add(val a: Int, val b: Int, val total: Int) : Flash()
    data class Take(val n: Int, val b: Int, val left: Int) : Flash()
}

data class UiModel(
    val screen: Screen,
    val copy: Copy,
    val muted: Boolean,
    val settingsOpen: Boolean,
    /** True until a language has ever been chosen: shows the first-run picker. */
    val firstRun: Boolean,
    /** False when the device lacks TTS voice data for the chosen language. */
    val voiceAvailable: Boolean = true,
    /** False while the TTS engine has not yet bound: the missing-voice note
     *  must wait for it, or every cold start scolds a healthy device. */
    val voiceReady: Boolean = true,
    val flash: Flash? = null,
    /** Increments to fire one confetti burst. */
    val confettiKey: Int = 0,
)
