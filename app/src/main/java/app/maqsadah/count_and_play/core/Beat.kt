package app.maqsadah.count_and_play.core

/** Sound effects the host can play. */
enum class Sfx { TICK, CLINK, THUD, CHIME, RUSTLE }

/**
 * Everything the core can ask the host to do, in order. Pure data: no speech
 * engine, no sound pool, no Android. The host decides how each beat is
 * performed and how beats are queued.
 */
sealed class Beat {
    /** Say the counting word for [n] ("one", "two", ...). */
    data class SayCount(val n: Int) : Beat()

    /** Say the cardinal answer: how many there are in all. */
    data class SayCardinal(val n: Int) : Beat()

    /** Short praise for a finished count: "Well done!" / "সাব্বাশ!" */
    object SayPraise : Beat()

    /** Round-start prompt for COUNT; also opens ADD, whose first job is to count each plate. */
    object SayPromptCount : Beat()

    /** Both ADD plates are counted, the button is awake: "Put them together!" */
    object SayPromptAdd : Beat()

    /** ADD, after the pour: the whole is counted afresh: "Count them all!" */
    object SayPromptAll : Beat()

    /** Round-start prompt for TAKE: "take away [b]". */
    data class SayPromptTake(val b: Int) : Beat()

    /** TAKE, once the b are gone: "How many are left?" */
    object SayPromptLeft : Beat()

    /** "a and b make total". */
    data class SayFactAdd(val a: Int, val b: Int, val total: Int) : Beat()

    /** "n take away b leaves left". */
    data class SayFactTake(val n: Int, val b: Int, val left: Int) : Beat()

    /** Play one sound effect. */
    data class Play(val sfx: Sfx) : Beat()

    /** Show the "n!" number card for COUNT. */
    data class FlashCount(val n: Int) : Beat()

    /** Show the "a + b = total" card for ADD. */
    data class FlashAdd(val a: Int, val b: Int, val total: Int) : Beat()

    /** Show the "n - b = left" card for TAKE. */
    data class FlashTake(val n: Int, val b: Int, val left: Int) : Beat()

    /** Celebration particles. */
    object Confetti : Beat()
}
