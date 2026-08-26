package app.maqsadah.count_and_play.copy

/** The languages the app plays in. [copyOf] is the one door from a language to its words. */
enum class Language { EN, BN }

/**
 * Words. The game decides what is said; each language decides how it is worded.
 *
 * Android string resources are the wrong tool here for a specific reason:
 * Bengali's plural rules have only an `other` category, so `<plurals>` buys
 * nothing, and the real grammatical difference is a *classifier*: টি attaches
 * to the numeral and the noun stays unchanged ("তিনটি আপেল"), which no resource
 * mechanism models. These lines are grammar, not text, so each language writes
 * its own templates instead of swapping words into a shared one.
 */
interface Copy {
    /** The bare number word, as spoken while tagging objects: "three" / "তিন". Defined for 0..20. */
    fun numberWord(n: Int): String

    /** On-screen numerals in this language's own digits: 12 -> "12" / "১২". */
    fun digits(n: Int): String

    /**
     * What the pieces are called, keyed by the ShapeKind's NAME (e.g. "APPLE"),
     * so this layer does not depend on core. The noun is singular and stays
     * unchanged; Bengali never pluralizes it.
     */
    fun itemName(shapeName: String): String

    /**
     * What one countable says to a screen reader: its name, and when it has
     * been counted, its order in the child's own count.
     */
    fun objectLabel(shapeName: String, countOrder: Int): String

    /** The home shelf's question: "What shall we play?" */
    fun homeTitle(): String

    /** The three activity tiles. */
    fun tileCount(): String
    fun tileAdd(): String
    fun tileTake(): String

    /**
     * Instructions, one per activity. Fixed phrasings, never varied: a 3-year-old
     * wants the same words every time, because that is how it becomes a routine
     * he can predict and eventually stop needing.
     *
     * These four render on screen as well as being spoken, so they carry no
     * exclamation marks: the calm line is the confident one. Spoken-only lines
     * (praise, cardinals, facts) keep theirs for voice intonation alone.
     * The vocabulary is deliberately tiny and repeats across games (tap,
     * count, together, take away, left): repetition is how these become the
     * first words he reads.
     */
    fun promptCount(): String
    fun promptAdd(): String
    fun promptTake(b: Int): String

    /** ADD, after the pour: the whole is counted afresh: "Count them all". */
    fun promptAll(): String

    /** TAKE, once the asked number is gone: "How many are left?" */
    fun promptLeft(): String

    /** The answer to "how many", as a spoken exclamation: "Four!" */
    fun cardinal(n: Int): String

    /** Short praise when a COUNT round is finished: "Well done!" / "সাব্বাশ!" */
    fun praise(): String

    /**
     * The plain arithmetic fact, spoken warmly: the statement itself is better
     * praise than anything evaluative: "Three and two make five!"
     */
    fun factAdd(a: Int, b: Int, total: Int): String
    fun factTake(n: Int, b: Int, left: Int): String

    // -- Grown-up chrome (icon labels for screen readers, settings copy) ----

    /** Each language's own name, written in itself: "English" / "বাংলা". */
    fun languageName(language: Language): String

    /**
     * The first-run picker shows both titles at once, before any language is
     * chosen, so both packs carry both lines identically.
     */
    fun firstRunTitleEn(): String
    fun firstRunTitleBn(): String

    /** Grown-up note shown when the device lacks voice data for the chosen language. */
    fun voiceMissingNote(): String

    /** Labels for the icon-only controls, for screen readers. */
    fun homeLabel(): String
    fun settingsLabel(): String
    fun closeLabel(): String
    fun soundOnLabel(): String
    fun soundOffLabel(): String

    /** State of the pour button, announced alongside its label. */
    fun pourReadyState(): String
    fun pourNotYetState(): String
}

/** The one entry point: a language, and the words that go with it. */
fun copyOf(language: Language): Copy = when (language) {
    Language.EN -> EnCopy
    Language.BN -> BnCopy
}
