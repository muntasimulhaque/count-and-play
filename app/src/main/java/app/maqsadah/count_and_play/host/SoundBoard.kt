package app.maqsadah.count_and_play.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import app.maqsadah.count_and_play.R
import app.maqsadah.count_and_play.core.Sfx
import java.util.Collections

/**
 * SoundPool rather than MediaPlayer, deliberately: MediaPlayer's start latency
 * runs past 100 ms, and past about 100 ms a 3-year-old no longer perceives the
 * sound as *caused by their finger*. The tap sound only works as the immediate
 * physical consequence of the child's own action.
 */
class SoundBoard(context: Context) {

    private val app = context.applicationContext

    /** Sample ids, filled in as each load is requested. */
    private val ids = Collections.synchronizedMap(HashMap<Sfx, Int>())
    private val bySample = Collections.synchronizedMap(HashMap<Int, Sfx>())
    private val readySamples = Collections.synchronizedSet(HashSet<Int>())

    /** Requests that arrived before their sample decoded, replayed on load. */
    private val pending = Collections.synchronizedSet(HashSet<Sfx>())

    @Volatile private var released = false

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        // The listener goes on BEFORE the first load. SoundPool decodes on its
        // own thread: a tiny sample that finished before the listener existed
        // would never be marked ready, and the first tap of a round would be
        // silent for the rest of the session.
        runCatching {
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                runCatching {
                    if (status != 0) return@setOnLoadCompleteListener
                    readySamples += sampleId
                    val wanted = bySample[sampleId] ?: return@setOnLoadCompleteListener
                    if (pending.remove(wanted)) playNow(wanted)
                }
            }
        }
        load(Sfx.TICK, R.raw.sfx_tick)
        load(Sfx.THUD, R.raw.sfx_thud)
        load(Sfx.CHIME, R.raw.sfx_chime)
        load(Sfx.RUSTLE, R.raw.sfx_rustle)
    }

    private fun load(sfx: Sfx, resId: Int) {
        val id = runCatching { pool.load(app, resId, 1) }.getOrDefault(0)
        ids[sfx] = id
        if (id != 0) bySample[id] = sfx
    }

    @Volatile private var lastChimeAt = 0L

    fun play(sfx: Sfx) {
        if (released) return
        val id = ids[sfx] ?: return
        // Two pitched notes in quick succession make an interval, and intervals
        // are where melody starts. The flow keeps chimes seconds apart already;
        // this is the structural guarantee.
        if (sfx == Sfx.CHIME) {
            val now = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
            if (now - lastChimeAt < CHIME_GAP_MS) return
            lastChimeAt = now
        }
        if (id == 0 || id !in readySamples) {
            pending.add(sfx)
            return
        }
        playNow(sfx)
    }

    private fun playNow(sfx: Sfx) {
        if (released) return
        val id = ids[sfx] ?: return
        if (id == 0) return
        val volume = volumeOf(sfx)
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    /** Effects duck under the voice so a number word is never masked. */
    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.TICK -> 0.85f
        Sfx.THUD -> 0.75f
        Sfx.CHIME -> 0.80f
        Sfx.RUSTLE -> 0.60f
    }

    fun release() {
        if (released) return
        released = true
        runCatching { pending.clear() }
        runCatching { bySample.clear() }
        runCatching { pool.release() }
    }

    private companion object {
        const val CHIME_GAP_MS = 1200L
    }
}
