package app.maqsadah.count_and_play.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import app.maqsadah.count_and_play.R
import app.maqsadah.count_and_play.core.Sfx

/**
 * SoundPool rather than MediaPlayer, deliberately: MediaPlayer's start latency
 * runs past 100 ms, and past about 100 ms a 3-year-old no longer perceives the
 * sound as *caused by their finger*. The tap sound only works as the immediate
 * physical consequence of the child's own action.
 */
class SoundBoard(context: Context) {

    private val app = context.applicationContext

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = mapOf(
        Sfx.TICK to pool.load(app, R.raw.sfx_tick, 1),
        Sfx.CLINK to pool.load(app, R.raw.sfx_clink, 1),
        Sfx.THUD to pool.load(app, R.raw.sfx_thud, 1),
        Sfx.CHIME to pool.load(app, R.raw.sfx_chime, 1),
        Sfx.RUSTLE to pool.load(app, R.raw.sfx_rustle, 1),
    )

    private var lastChimeAt = 0L

    fun play(sfx: Sfx) {
        val id = loaded[sfx] ?: return
        // Two pitched notes in quick succession make an interval, and intervals
        // are where melody starts. The flow keeps chimes seconds apart already;
        // this is the structural guarantee.
        if (sfx == Sfx.CHIME) {
            val now = System.currentTimeMillis()
            if (now - lastChimeAt < 1200) return
            lastChimeAt = now
        }
        val volume = volumeOf(sfx)
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** Effects duck under the voice so a number word is never masked. */
    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.TICK -> 0.85f
        Sfx.CLINK -> 0.70f
        Sfx.THUD -> 0.75f
        Sfx.CHIME -> 0.80f
        Sfx.RUSTLE -> 0.60f
    }

    fun release() = pool.release()
}
