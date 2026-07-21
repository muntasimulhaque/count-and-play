package app.maqsadah.count_and_play

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** One installed English TTS voice, shown in the Grown-ups settings. */
data class TtsVoice(val name: String, val label: String)

/**
 * Suspend-friendly wrapper around Android's TextToSpeech (en-US, pitch 1.15,
 * completion callbacks + safety timeout because some engines never fire them).
 *
 * v3: the grown-ups settings can pick one of the device's installed English
 * voices and a Slow/Normal speech rate; both persist in SharedPreferences and
 * are applied on init. All Voice APIs are guarded — some engines throw.
 */
class Speaker(context: Context) {

    var soundOn by mutableStateOf(true)
        private set

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(G.PREFS, Context.MODE_PRIVATE)

    /** Installed English voices (filled once the TTS engine is ready). */
    val voices = mutableStateListOf<TtsVoice>()

    /** Persisted voice choice (null = engine default). */
    var voiceName by mutableStateOf(prefs.getString(G.KEY_VOICE, null))
        private set

    /** Persisted rate choice: Slow (0.7) vs Normal (0.9). */
    var slowRate by mutableStateOf(prefs.getBoolean(G.KEY_SLOW, false))
        private set

    private var ready = false
    private var tts: TextToSpeech? = null
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    private fun currentRate() = if (slowRate) 0.7f else 0.9f

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                // Prefer en-US; if that voice data is missing, fall back to the
                // device's default locale so narration still works out of the box
                // (the numbers/words are English, but any voice is better than silence).
                var lang = t.setLanguage(Locale.US)
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    lang = t.setLanguage(Locale.getDefault())
                }
                if (lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED) {
                    t.setSpeechRate(currentRate())
                    t.setPitch(1.15f)
                    applyStoredVoice(t)
                    loadVoices(t)
                    t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) = finish(utteranceId)
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) = finish(utteranceId)
                        override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
                    })
                    ready = true
                }
            }
        }
    }

    /** Applies the persisted voice (if any) to [t]. Guarded: Voice APIs throw on some engines. */
    private fun applyStoredVoice(t: TextToSpeech) {
        val name = voiceName ?: return
        try {
            val v = t.voices?.firstOrNull { it.name == name } ?: return
            t.voice = v
        } catch (_: Exception) {
            // engine rejected the voice — keep the default
        }
    }

    /** Lists installed local English voices with friendly, numbered labels. */
    private fun loadVoices(t: TextToSpeech) {
        try {
            val english = t.voices
                ?.filter { it.locale.language == Locale.ENGLISH.language && !it.isNetworkConnectionRequired }
                ?: emptyList()
            val list = mutableListOf<TtsVoice>()
            english.groupBy { it.locale.displayName }.toSortedMap().forEach { (locName, vs) ->
                vs.sortedBy { it.name }.forEachIndexed { i, v ->
                    val label = if (vs.size == 1) locName else "$locName ${i + 1}"
                    list.add(TtsVoice(v.name, label))
                }
            }
            voices.clear()
            voices.addAll(list)
        } catch (_: Exception) {
            // voices unavailable — the picker just shows the default option
        }
    }

    /** Selects (or clears, with null) the narration voice; persists the choice. */
    fun selectVoice(name: String?) {
        voiceName = name
        prefs.edit().putString(G.KEY_VOICE, name).apply()
        val t = tts ?: return
        try {
            if (name == null) {
                t.defaultVoice?.let { t.voice = it }
            } else {
                val v = t.voices?.firstOrNull { it.name == name }
                if (v != null) t.voice = v
            }
        } catch (_: Exception) {
            // keep whatever voice the engine had
        }
    }

    /** Sets Slow (true) or Normal (false) speech rate; persists the choice. */
    fun setSlowRate(slow: Boolean) {
        slowRate = slow
        prefs.edit().putBoolean(G.KEY_SLOW, slow).apply()
        try {
            tts?.setSpeechRate(currentRate())
        } catch (_: Exception) {
        }
    }

    private fun finish(id: String?) {
        val cont = id?.let { pending.remove(it) } ?: return
        try {
            cont.resume(Unit)
        } catch (_: IllegalStateException) {
            // already resumed/cancelled — safe to ignore
        }
    }

    fun toggle() {
        soundOn = !soundOn
        if (!soundOn) tts?.stop()
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    /** Speaks [text] and suspends until done (or a length-based timeout). */
    suspend fun speak(text: String) {
        val t = tts
        if (!soundOn || !ready || t == null) {
            delay(350)
            return
        }
        withTimeoutOrNull(maxOf(1600L, text.length * 130L)) {
            suspendCancellableCoroutine { cont ->
                val id = UUID.randomUUID().toString()
                pending[id] = cont
                cont.invokeOnCancellation {
                    pending.remove(id)
                    t.stop()
                }
                val res = t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                if (res != TextToSpeech.SUCCESS) finish(id)
            }
        }
    }
}
