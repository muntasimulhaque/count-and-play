package app.maqsadah.count_and_play.copy

import app.maqsadah.count_and_play.core.Line
import app.maqsadah.count_and_play.core.ShapeKind

enum class Language(val tag: String, val nativeName: String) {
    EN("en", "English"),
    BN("bn", "বাংলা"),
}

/**
 * Words. The domain decides what is said; this decides how it is worded.
 *
 * Android string resources are the wrong tool here for a specific reason:
 * Bengali's plural rules have only an `other` category, so `<plurals>` buys
 * nothing, and the real grammatical difference is a *classifier* — টা attaches
 * to the numeral and the noun stays unchanged — which no resource mechanism
 * models. Narration lines are grammar, not text.
 */
interface Copy {
    val language: Language

    /** The count word alone, as spoken while tagging objects. */
    fun countWord(n: Int): String

    /** On-screen numerals. Bengali draws ০-৯, English 0-9. */
    fun digits(n: Int): String

    fun noun(shape: ShapeKind, count: Int): String

    fun speak(line: Line): String

    val ui: UiText
}

/** The few strings a grown-up reads. Adult register, unlike everything else. */
interface UiText {
    val play: String
    val settings: String
    val language: String
    val voice: String
    val defaultVoice: String
    val slowVoice: String
    val normalVoice: String
    val sound: String
    val soundOn: String
    val soundOff: String
    val resetProgress: String
    val areYouSure: String
    val done: String
    val next: String
    val chooseShape: String
    val grownUps: String
    val gateHint: String
    val noVoiceInstalled: String
    val sessionOver: String
    val playAgain: String
    val whatTheyreLearning: String
    fun skillLine(skill: String, level: Int): String
}

fun copyFor(language: Language): Copy = when (language) {
    Language.EN -> CopyEn
    Language.BN -> CopyBn
}
