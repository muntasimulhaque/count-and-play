package app.maqsadah.count_and_play.host

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.maqsadah.count_and_play.copy.Language
import java.util.Locale

/**
 * Speech, paced by the host on [speaking]: an utterance is fired and the host
 * then waits until the engine reports it done (bounded, so a wedged engine
 * cannot stall a round). What still holds from the old build: every utterance
 * is epoch-tagged so a late engine callback can never be mistaken for live
 * ones, stopping bumps the epoch before the engine halts, and a foreground/
 * mute gate makes speak a silent no-op whenever the app should not be talking.
 */
class Narrator(application: Application, initialLanguage: Language) {

    private var language = initialLanguage
    private var ready = false
    private var muted = false
    private var foreground = true
    private var epoch = 0L
    private var counter = 0L
    private var queued: String? = null
    private var engine: TextToSpeech? = null
    private val pending = HashSet<String>()

    /** False when this device has no voice data for the chosen language. */
    var voiceAvailable = false
        private set

    /** True while the engine is speaking an utterance from the current epoch. */
    val speaking: Boolean get() = pending.isNotEmpty()

    init {
        engine = TextToSpeech(application.applicationContext) { status ->
            // The ViewModel can be cleared before the engine binds; a callback
            // arriving after release() must not resurrect state.
            if (engine == null) return@TextToSpeech
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.setSpeechRate(RATE)
                applyLanguage(language)
                // The first prompt can arrive before the engine binds; saying
                // it late beats the old failure of a muted first round.
                queued?.let { queued = null; speak(it) }
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finish(utteranceId)

                @Deprecated("required by the framework")
                override fun onError(utteranceId: String?) = finish(utteranceId)
                override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
            })
        }
    }

    /** Epoch guard: ids tagged before the last stop() are stale and dropped. */
    private fun finish(utteranceId: String?) {
        val id = utteranceId ?: return
        if (id.substringBefore(':').toLongOrNull() != epoch) return
        pending.remove(id)
    }

    fun speak(text: String) {
        if (!foreground || muted) return
        val tts = engine ?: return
        if (!ready) {
            queued = text
            return
        }
        val id = "$epoch:${counter++}"
        pending.add(id)
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
        }
    }

    /** cancelAndJoin-style stop: the epoch bump stale-s every in-flight
     *  utterance the moment the engine halts. */
    fun stop() {
        epoch++
        pending.clear()
        queued = null
        engine?.stop()
    }

    fun setLanguage(language: Language) {
        this.language = language
        applyLanguage(language)
    }

    private fun applyLanguage(language: Language) {
        val tts = engine ?: return
        val result = tts.setLanguage(Locale.forLanguageTag(language.name.lowercase()))
        voiceAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        // A device without this voice gets the default voice rather than silence.
        if (!voiceAvailable) tts.setLanguage(Locale.getDefault())
    }

    /** Lifecycle gate: a backgrounded app stops talking, dead. */
    fun pause() {
        foreground = false
        stop()
    }

    fun resume() {
        foreground = true
    }

    fun setMuted(muted: Boolean) {
        this.muted = muted
        if (muted) stop()
    }

    fun release() {
        stop()
        engine?.shutdown()
        engine = null
    }

    private companion object {
        // ~0.85 matches the 110-130 wpm a 3-year-old actually parses.
        const val RATE = 0.85f
    }
}
