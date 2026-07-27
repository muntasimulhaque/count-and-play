package app.maqsadah.count_and_play.host

import app.maqsadah.count_and_play.core.Beat
import app.maqsadah.count_and_play.core.Line
import app.maqsadah.count_and_play.core.Pace
import app.maqsadah.count_and_play.core.Script
import app.maqsadah.count_and_play.core.Sfx
import app.maqsadah.count_and_play.core.StageChange
import app.maqsadah.count_and_play.speech.Narrator
import kotlinx.coroutines.delay

/** Where time passes. Swapped for [Instant] in tests so choreography runs in
 *  microseconds and no test ever sleeps. */
interface Timing {
    suspend fun wait(millis: Long)

    object Real : Timing {
        override suspend fun wait(millis: Long) = delay(millis)
    }

    object Instant : Timing {
        override suspend fun wait(millis: Long) = Unit
    }
}

/**
 * The only place in the app permitted to call `delay()`.
 *
 * The old build scattered fourteen hardcoded delays through its ViewModel,
 * beside the speech calls and the persistence writes. Confining them here is
 * what lets the rules stay pure and the whole app be tested without a clock.
 */
class ScriptRunner(
    private val narrator: () -> Narrator,
    private val timing: Timing,
    private val onSound: (Sfx) -> Unit,
    private val onChange: (StageChange) -> Unit,
    private val render: (Line) -> String,
) {
    suspend fun play(script: Script) {
        for (beat in script.beats) {
            when (beat) {
                is Beat.Say -> narrator().say(render(beat.line), beat.settled)
                is Beat.Cue -> onSound(beat.sound)
                is Beat.Show -> {
                    onChange(beat.change)
                    timing.wait(Motion.durationOf(beat.change))
                }
                is Beat.Pause -> timing.wait(beat.pace.millis)
            }
        }
    }
}

/** Every named silence, in one table, so none of them drifts. */
val Pace.duration: Long get() = millis
