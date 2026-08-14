package app.maqsadah.count_and_play.copy

/**
 * Bengali, carrying over the native-speaker-polished voice from v3.4 verbatim
 * wherever a line survived.
 *
 * Grammar, as the old pack had it:
 * - While counting, the words are bare: এক, দুই, তিন. A standalone exclamation
 *   stays bare too ("চার!"), exactly like the old pack's praise lines.
 * - The noun never changes; the classifier টি attaches to the numeral:
 *   "তিনটি আপেল", "দুইটি বাদ দাও!"
 * - The digits on screen are Bengali: ০-৯.
 * - Subtraction has two registers. The instruction to the child says বাদ দাও
 *   ("দুইটি বাদ দাও!"), but the arithmetic term (standalone and in the fact
 *   lines) is বিয়োগ. That was the v3.4 native-eye correction (it replaced বাদ),
 *   and the fact pair keeps the old pack's "...যোগ... হয়..." skeleton:
 *   "তিন যোগ দুই হয় পাঁচ!" / "পাঁচ বিয়োগ দুই হয় তিন!"
 */
object BnCopy : Copy {

    /** The old BnPack's number words, 0..20, verbatim. */
    private val words = listOf(
        "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
        "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোল", "সতেরো", "আঠারো", "উনিশ", "বিশ",
    )

    private val numerals = "০১২৩৪৫৬৭৮৯"

    /** The ten shapes' item names, ported from the old Bengali pack. */
    private val items = mapOf(
        "APPLE" to "আপেল",
        "PEAR" to "নাশপাতি",
        "STAR" to "তারা",
        "LEAF" to "পাতা",
        "BLOCK" to "ব্লক",
        "BEAD" to "পুঁতি",
        "MELON" to "তরমুজ",
        "CARROT" to "গাজর",
        "TULIP" to "টিউলিপ",
        "BALL" to "বল",
    )

    override fun numberWord(n: Int): String = words.getOrElse(n) { digits(n) }

    override fun digits(n: Int): String =
        n.toString().map { c -> if (c in '0'..'9') numerals[c - '0'] else c }.joinToString("")

    override fun itemName(shapeName: String): String = items[shapeName] ?: "things"

    override fun homeTitle(): String = "কী খেলবো?"

    override fun tileCount(): String = "গুনে দেখি"
    override fun tileAdd(): String = "একসাথে করি"
    override fun tileTake(): String = "বের করি"

    override fun promptCount(): String = "ট্যাপ করে গুনো!"
    override fun promptAdd(): String = "একসাথে করো!"
    override fun promptTake(b: Int): String = "${numberWord(b)}টি বাদ দাও!"

    override fun promptAll(): String = "সবগুলো গুনো!"
    override fun promptLeft(): String = "কতগুলো রইলো?"

    override fun cardinal(n: Int): String = "${numberWord(n)}!"

    override fun factAdd(a: Int, b: Int, total: Int): String =
        "${numberWord(a)} যোগ ${numberWord(b)} হয় ${numberWord(total)}!"

    override fun factTake(n: Int, b: Int, left: Int): String =
        "${numberWord(n)} বিয়োগ ${numberWord(b)} হয় ${numberWord(left)}!"

    override fun celebrate(): String = "সাবাস!"
}
