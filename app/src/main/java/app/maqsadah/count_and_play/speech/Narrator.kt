package app.maqsadah.count_and_play.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import app.maqsadah.count_and_play.copy.Language
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Speech, as something that can be awaited.
 *
 * The old build's counting was destroyed by two independent mechanisms: a bare
 * `job?.cancel()` (which returns *before* cleanup runs, so two voices overlap)
 * and `QUEUE_FLUSH` on every utterance (so each tap truncated the previous
 * number word mid-syllable). A child tapping at a natural two taps per second
 * heard "thr—", "fo—", "fi—" — and a truncated number word is worse than
 * silence, because it maps a phonological fragment onto a quantity.
 *
 * Here there is no queue at all: one script plays at a time, each utterance is
 * awaited to completion, and cancellation is joined before the next begins.
 */
interface Narrator {
    val voiceAvailable: Boolean
    suspend fun say(text: String, settled: Boolean)
    fun stopNow()
    fun release()
}

/** Used when muted, when no engine exists, and in tests. Paces identically, so
 *  a silent playthrough has the same rhythm and is not a separate code path. */
class SilentNarrator(private val realTime: Boolean = true) : Narrator {
    override val voiceAvailable = false

    override suspend fun say(text: String, settled: Boolean) {
        if (!realTime) return
        // Roughly the time the words would have taken, so timing does not
        // change when the sound is off.
        val words = text.count { it == ' ' } + 1
        delay(260L + words * 240L)
    }

    override fun stopNow() = Unit
    override fun release() = Unit
}

class TtsNarrator(
    context: Context,
    language: Language,
) : Narrator {

    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private var epoch = 0L
    private var counter = 0L
    private var ready = false
    private var engine: TextToSpeech? = null

    override var voiceAvailable: Boolean = false
        private set

    var slowRate: Boolean = false

    private val readyGate = CompletableDeferred<Unit>()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) applyLanguage(language)
            // The old build left `ready` false until this callback fired and
            // made every `speak()` a silent 350 ms delay, so the entire first
            // round ran mute and at a third of its proper pace. Waiting on a
            // gate instead means the first round simply starts a moment later,
            // narrated correctly.
            readyGate.complete(Unit)
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

    /** Epoch-tagged, so a slow engine's late callback cannot resume an
     *  utterance that has since been replaced. */
    private fun finish(utteranceId: String?) {
        utteranceId?.let { pending.remove(it)?.complete(Unit) }
    }

    fun applyLanguage(language: Language) {
        val tts = engine ?: return
        val locale = Locale.forLanguageTag(language.tag)
        val result = tts.setLanguage(locale)
        voiceAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!voiceAvailable) tts.language = Locale.getDefault()
    }

    fun voices(language: Language): List<Voice> =
        engine?.voices.orEmpty()
            .filter { it.locale.language == language.tag }
            // Offline first: this app has no network permission at all, and
            // Bengali voices on Google TTS are frequently network-only.
            .sortedBy { it.isNetworkConnectionRequired }

    fun useVoice(name: String?) {
        val tts = engine ?: return
        val chosen = tts.voices.orEmpty().firstOrNull { it.name == name } ?: return
        tts.voice = chosen
    }

    /**
     * Speaks, and **can never hang**.
     *
     * A script is a sequence of beats, and the beats after an utterance include
     * the pause and the badge collapse that carry the actual teaching. If an
     * engine never fires its completion callback — no TTS installed, no voice
     * data, a wedged service — an unbounded await would strand the child on a
     * half-finished round with no way forward. So every utterance gets a budget
     * derived from its own length, and the script moves on regardless.
     */
    override suspend fun say(text: String, settled: Boolean) {
        withTimeoutOrNull(budgetFor(text)) { speakAwaiting(text, settled) }
    }

    private fun budgetFor(text: String): Long {
        val words = text.count { it == ' ' } + 1
        return 700L + words * 420L
    }

    private suspend fun speakAwaiting(text: String, settled: Boolean) {
        readyGate.await()
        val tts = engine ?: return
        if (!ready) return

        val base = if (slowRate) SLOW_RATE else NORMAL_RATE
        // The cardinal is spoken lower and slower — that prosodic drop is what
        // marks the last count word as the answer rather than the next tag.
        tts.setSpeechRate(if (settled) base - 0.12f else base)
        tts.setPitch(if (settled) 0.94f else 1.0f)

        val id = "${epoch}:${counter++}"
        val signal = CompletableDeferred<Unit>()
        pending[id] = signal

        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                pending.remove(id)
                tts.stop()
            }
            val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (queued != TextToSpeech.SUCCESS) {
                pending.remove(id)
                signal.complete(Unit)
            }
            cont.resume(Unit)
        }
        signal.await()
    }

    override fun stopNow() {
        epoch++
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
        engine?.stop()
    }

    override fun release() {
        stopNow()
        engine?.shutdown()
        engine = null
    }

    private companion object {
        /** 0.80-0.85 matches the 110-130 wpm a 3-year-old actually parses.
         *  The old build ran at 0.9, which is adult conversational pace. */
        const val NORMAL_RATE = 0.84f
        const val SLOW_RATE = 0.72f
    }
}
