package app.maqsadah.count_and_play

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
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

/**
 * Suspend-friendly wrapper around Android's TextToSpeech, mirroring the
 * original web app's speak() helper (en-US, rate 0.85, pitch 1.15, with a
 * safety timeout because some engines never fire completion callbacks).
 */
class Speaker(context: Context) {

    var soundOn by mutableStateOf(true)
        private set

    private var ready = false
    private var tts: TextToSpeech? = null
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
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
                    t.setSpeechRate(0.85f)
                    t.setPitch(1.15f)
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
