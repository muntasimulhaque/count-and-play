package app.maqsadah.count_and_play.copy

import app.maqsadah.count_and_play.core.Line
import app.maqsadah.count_and_play.core.ShapeKind

/**
 * Bengali, and here Bengali is genuinely better at this than English.
 *
 * While counting, the words are bare: এক, দুই, তিন. The cardinal takes a
 * classifier: **তিনটা** আপেল. So the moment the last count word changes status
 * from "the next tag" to "how many there are" is marked *morphologically* — a
 * Bengali-speaking child gets an audible cue to cardinality that an English one
 * simply does not, since English says "three" both times.
 *
 * The classifier therefore appears only on the cardinal, never during a count.
 *
 * Register: টা, not টি. টি is the written/standard form; টা is what a
 * Bangladeshi parent actually says to a toddler. The difference is whether the
 * app sounds like a textbook or like a parent.
 */
object CopyBn : Copy {
    override val language = Language.BN

    private val words = listOf(
        "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ",
        "ছয়", "সাত", "আট", "নয়", "দশ",
    )

    private val numerals = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    override fun countWord(n: Int) = words.getOrElse(n) { digits(n) }

    override fun digits(n: Int) =
        n.toString().map { if (it.isDigit()) numerals[it - '0'] else it }.joinToString("")

    /** The count word with its classifier — the cardinal form. */
    private fun counted(n: Int) = if (n == 0) words[0] else "${countWord(n)}টা"

    override fun noun(shape: ShapeKind, count: Int) = when (shape) {
        ShapeKind.APPLE -> "আপেল"
        ShapeKind.PEAR -> "নাশপাতি"
        ShapeKind.STAR -> "তারা"
        ShapeKind.LEAF -> "পাতা"
        ShapeKind.BLOCK -> "ব্লক"
        ShapeKind.BEAD -> "পুঁতি"
        ShapeKind.MELON -> "তরমুজ"
        ShapeKind.CARROT -> "গাজর"
        ShapeKind.TULIP -> "টিউলিপ"
        ShapeKind.BALL -> "বল"
    }

    override fun speak(line: Line): String = when (line) {
        // Bare while counting…
        is Line.CountWord -> countWord(line.n)
        // …classified on the cardinal. This contrast is the whole point.
        is Line.Cardinal -> "${counted(line.n)} ${noun(line.shape, line.n)}।"
        Line.CountThem -> "গুনে দেখো।"
        Line.HowMany -> "কয়টা?"

        // দাও (give), not রাখো (place) — warm, and exactly the Give-N framing.
        is Line.GiveN -> "বাটিতে ${counted(line.n)} ${noun(line.shape, line.n)} দাও।"
        is Line.GaveIt -> "${counted(line.n)}! পেরেছো।"
        Line.LetsCount -> "চলো গুনে দেখি।"
        is Line.TooMany -> "${counted(line.got)} হয়ে গেছে। ${counted(line.wanted)} দরকার।"

        Line.WhichHasMore -> "কোনটায় বেশি?"
        is Line.ThisHasMore -> "এটায় ${counted(line.n)}।"

        Line.WhatsUnder -> "পাতার নিচে কয়টা?"
        Line.MakeItHere -> "এখানে বানাও।"

        // আর ("and"), never যোগ ("addition") — যোগ is a school term a 3-year-old
        // has no use for, and it makes the Bengali formal where the English is
        // casual. তিন আর দুই is how a parent actually says it.
        is Line.PartsNamed -> "${counted(line.a)}, আর ${counted(line.b)}।"
        Line.HowManyAltogether -> "কয়টা হবে?"
        Line.AllTogetherNow -> "এবার সব একসাথে।"
        is Line.MakesTotal ->
            "${counted(line.total)}! ${countWord(line.a)} আর ${countWord(line.b)} মিলে ${countWord(line.total)}।"
        is Line.AndBackAgain -> "আবার আলাদা। ${countWord(line.a)}, আর ${countWord(line.b)}।"

        is Line.TakeOut -> "${counted(line.n)} বের করো।"
        Line.HowManyLeft -> "কয়টা রইলো?"
        is Line.WeMade -> "আমরা ${counted(line.n)} বানালাম।"
        Line.NothingLeft -> "কিছু নেই। শূন্য।"

        Line.NudgeGentle -> "তোমার পালা।"
        is Line.NudgeModel -> "দেখো। ${countWord(line.n)}।"

        Line.SessionDone -> "আজ এটুকুই।"
    }

    override val ui = object : UiText {
        override val play = "খেলি"
        override val settings = "সেটিংস"
        override val language = "ভাষা"
        override val voice = "স্বর"
        override val defaultVoice = "ডিফল্ট স্বর"
        override val slowVoice = "ধীরে"
        override val normalVoice = "স্বাভাবিক"
        override val sound = "শব্দ"
        override val soundOn = "চালু"
        override val soundOff = "বন্ধ"
        override val resetProgress = "নতুন করে শুরু"
        override val areYouSure = "নিশ্চিত?"
        override val done = "হয়েছে"
        override val next = "এরপর"
        override val chooseShape = "কী গুনবো?"
        override val grownUps = "বড়দের জন্য"
        override val gateHint = "খুলতে চেপে ধরে রাখুন"
        override val noVoiceInstalled =
            "এই ডিভাইসে এই ভাষার স্বর নেই। অ্যাপ ঠিকই চলবে — প্রতিটি সংখ্যা পর্দায় দেখা যাবে।"
        override val sessionOver = "শেষ!"
        override val playAgain = "আবার"
        override val whatTheyreLearning = "ও যা শিখছে"
        override fun skillLine(skill: String, level: Int) = "$skill · ধাপ ${digits(level)}"
    }
}
