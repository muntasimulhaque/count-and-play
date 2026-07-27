package app.maqsadah.count_and_play.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import app.maqsadah.count_and_play.R
import app.maqsadah.count_and_play.core.Sfx

/**
 * The six effects.
 *
 * SoundPool rather than MediaPlayer, deliberately: MediaPlayer's start latency
 * runs past 100 ms, and past about 100 ms a 3-year-old no longer perceives the
 * sound as *caused by their finger*. The whole value of the tap sound is that
 * it is the immediate physical consequence of their own action.
 */
class SoundBoard(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = mutableMapOf<Sfx, Int>()
    private var lastChimeAt = 0L

    var enabled: Boolean = true

    init {
        val app = context.applicationContext
        loaded[Sfx.TICK] = pool.load(app, R.raw.sfx_tick, 1)
        loaded[Sfx.THUD] = pool.load(app, R.raw.sfx_thud, 1)
        loaded[Sfx.RUSTLE] = pool.load(app, R.raw.sfx_rustle, 1)
        loaded[Sfx.HOLLOW] = pool.load(app, R.raw.sfx_hollow, 1)
        loaded[Sfx.CLINK] = pool.load(app, R.raw.sfx_clink, 1)
        loaded[Sfx.CHIME] = pool.load(app, R.raw.sfx_chime, 1)
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        // Two pitched notes in a row make an interval, and intervals are where
        // melody begins. The constraint is enforced here rather than remembered.
        if (sfx == Sfx.CHIME) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastChimeAt < MIN_CHIME_GAP_MS) return
            lastChimeAt = now
        }
        val id = loaded[sfx] ?: return
        pool.play(id, volumeOf(sfx), volumeOf(sfx), 1, 0, 1f)
    }

    /** Effects duck under the voice so a number word is never masked. */
    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.TICK -> 0.85f
        Sfx.THUD -> 0.75f
        Sfx.RUSTLE -> 0.60f
        Sfx.HOLLOW -> 0.55f
        Sfx.CLINK -> 0.70f
        Sfx.CHIME -> 0.80f
    }

    fun release() = pool.release()

    private companion object {
        const val MIN_CHIME_GAP_MS = 1200L
    }
}
