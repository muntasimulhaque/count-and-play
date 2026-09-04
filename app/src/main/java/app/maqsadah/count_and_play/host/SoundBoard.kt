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

    private val loaded: Map<Sfx, Int>
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
        loaded = mapOf(
            Sfx.TICK to loadOrZero(R.raw.sfx_tick),
            Sfx.THUD to loadOrZero(R.raw.sfx_thud),
            Sfx.CHIME to loadOrZero(R.raw.sfx_chime),
            Sfx.RUSTLE to loadOrZero(R.raw.sfx_rustle),
        )
        // Registered after the load calls, so the listener only ever sees
        // sample ids this map already knows about.
        runCatching {
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                runCatching {
                    if (status != 0) return@setOnLoadCompleteListener
                    readySamples += sampleId
                    loaded.entries.firstOrNull { it.value == sampleId }?.key?.let { wanted ->
                        if (pending.remove(wanted)) playNow(wanted)
                    }
                }
            }
        }
    }

    private fun loadOrZero(resId: Int): Int =
        runCatching { pool.load(app, resId, 1) }.getOrDefault(0)

    @Volatile private var lastChimeAt = 0L

    fun play(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
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
        val id = loaded[sfx] ?: return
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
        runCatching { pool.release() }
    }

    private companion object {
        const val CHIME_GAP_MS = 1200L
    }
}
