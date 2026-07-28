package app.maqsadah.count_and_play.core

/**
 * Everything the app can say, as data rather than text.
 *
 * The domain decides *what* is said; `copy/` decides how it is worded in English
 * or Bengali. This matters more than it looks: Bengali marks cardinality
 * morphologically (bare `তিন` while counting, classifier `তিনটা` for the
 * cardinal), so the count word and the cardinal are genuinely different
 * grammatical objects, not one string with a number substituted in.
 */
sealed interface Line {
    /** A bare count word, spoken *after* the child's tap. Never before it. */
    data class CountWord(val n: Int) : Line

    /** The cardinal, after a deliberate pause, in a lower and slower voice. */
    data class Cardinal(val n: Int, val shape: ShapeKind) : Line

    data object CountThem : Line
    data object HowMany : Line

    /** The child sets the number before the round starts. He owns the problem. */
    data object PickHowMany : Line
    data object PickHowManyMore : Line
    data object PickHowManyAway : Line

    data class GiveN(val n: Int, val shape: ShapeKind) : Line
    data class GaveIt(val n: Int) : Line
    data object LetsCount : Line
    data class TooMany(val got: Int, val wanted: Int) : Line

    data object WhichHasMore : Line
    data class ThisHasMore(val n: Int) : Line

    data object WhatsUnder : Line
    data object MakeItHere : Line

    data class PartsNamed(val a: Int, val b: Int) : Line
    data object HowManyAltogether : Line
    data object AllTogetherNow : Line
    data class MakesTotal(val a: Int, val b: Int, val total: Int) : Line
    data class AndBackAgain(val a: Int, val b: Int) : Line

    data class TakeOut(val n: Int) : Line
    data object HowManyLeft : Line
    data class WeMade(val n: Int) : Line
    data object NothingLeft : Line

    /** The finite nudge ladder — never a loop, and it stops for good at the end. */
    data object NudgeGentle : Line
    data class NudgeModel(val n: Int) : Line

    data object SessionDone : Line
}

/**
 * The six sounds. All short, all real-world, none melodic.
 *
 * [CHIME] is the only pitched sound in the app and never plays twice inside
 * 1200 ms — two pitched notes in sequence would be an interval, and intervals
 * are the beginning of a melody.
 */
enum class Sfx { TICK, THUD, RUSTLE, HOLLOW, CLINK, CHIME }

/**
 * Named silences. Every one of these is load-bearing; do not compress them.
 *
 * [CARDINAL] in particular is the count-to-cardinal transition — the pause is
 * what separates "one, two, three" from "three berries", and that separation is
 * the whole of cardinality.
 */
enum class Pace(val millis: Long) {
    TINY(120),
    BEAT(320),
    CARDINAL(700),
    BREATH(900),
    SETTLE(1500),
}

/** A visible change the host animates. The script waits for it to finish. */
sealed interface StageChange {
    data class Drop(val ids: List<Int>) : StageChange
    data class Travel(val ids: List<Int>, val to: Zone) : StageChange
    data class Collapse(val zone: Zone, val total: Int) : StageChange
    data class Compact(val zone: Zone) : StageChange
    data class Cover(val zone: Zone) : StageChange
    data class Uncover(val zone: Zone) : StageChange

    /** A silent, spatial "here" — one object breathing, never all of them at once. */
    data class Highlight(val ids: List<Int>) : StageChange
    data object ShowPrediction : StageChange
    data object Celebrate : StageChange
}

sealed interface Beat {
    data class Say(val line: Line, val settled: Boolean = false) : Beat
    data class Cue(val sound: Sfx) : Beat
    data class Show(val change: StageChange) : Beat
    data class Pause(val pace: Pace) : Beat
}

/** An ordered run of beats. The host performs it; the domain never sleeps. */
@JvmInline
value class Script(val beats: List<Beat>) {
    val isEmpty: Boolean get() = beats.isEmpty()

    companion object {
        val none = Script(emptyList())
    }
}

fun script(build: ScriptBuilder.() -> Unit): Script =
    ScriptBuilder().apply(build).build()

class ScriptBuilder {
    private val beats = mutableListOf<Beat>()

    fun say(line: Line) { beats += Beat.Say(line) }

    /** The cardinal voice: lower, slower, settled. */
    fun settle(line: Line) { beats += Beat.Say(line, settled = true) }

    fun cue(sound: Sfx) { beats += Beat.Cue(sound) }
    fun show(change: StageChange) { beats += Beat.Show(change) }
    fun pause(pace: Pace) { beats += Beat.Pause(pace) }

    fun build() = Script(beats.toList())
}
