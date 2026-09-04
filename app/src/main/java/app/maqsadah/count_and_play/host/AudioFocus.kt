package app.maqsadah.count_and_play.host

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Audio focus, and nothing else. Transient-and-may-duck: the voice wins
 * briefly over music or video another app holds, and gives it back the moment
 * the app goes quiet. Needs no permission, so the zero-permission promise
 * stands.
 *
 * When someone else takes the mic mid-round, [setDucked] fires with true and
 * the voice stops mid-line; it stays quiet until a tap calls [request] again
 * and the ask is granted. A permanent loss drops the request object, so the
 * next tap asks afresh instead of assuming the loss is forever. A failed ask
 * (someone else is speaking) also answers false, so the app never talks over
 * their music; sounds keep playing, since a toy with no tick is a broken toy.
 */
class AudioFocus(
    private val application: Application,
    private val setDucked: (Boolean) -> Unit,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var request: AudioFocusRequest? = null

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        mainHandler.post {
            runCatching {
                setDucked(true)
                if (change == AudioManager.AUDIOFOCUS_LOSS) abandon()
            }
        }
    }

    /**
     * Asks to be heard. True: the voice may speak until the next loss. Under
     * API 26 there is no request API to honour, and the system ducks us on
     * its own, so the answer is simply yes.
     */
    fun request(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return runCatching {
            val manager = application.getSystemService(AudioManager::class.java)
                ?: return true
            val req = request ?: AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setOnAudioFocusChangeListener(listener, mainHandler)
                .build()
                .also { request = it }
            manager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }.getOrDefault(false)
    }

    /** Gives the mic back. Called on pause, on leaving a round, and on loss. */
    fun abandon() {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val req = request ?: return
            application.getSystemService(AudioManager::class.java)
                ?.abandonAudioFocusRequest(req)
            request = null
        }
    }
}
