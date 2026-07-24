package app.maqsadah.count_and_play

import java.util.Locale

/**
 * A language the app can run in. [nativeName] is shown (in its own script) on the
 * language picker so a grown-up can find their language without reading instructions.
 * [locale] drives the TTS engine + voice list in [Speaker].
 */
enum class Lang(val nativeName: String, val locale: Locale) {
    EN("English", Locale.US),
    BN("বাংলা", Locale("bn"));

    val pack: LangPack
        get() = when (this) {
            EN -> EnPack
            BN -> BnPack
        }
}

/**
 * Every user-facing string for one language, as template functions (not just word
 * lists) so each language controls its own grammar. English pluralizes nouns
 * ("three apples"); Bengali leaves the noun unchanged and adds the classifier "টি"
 * ("তিনটি আপেল"), so naive word-swapping would not work.
 *
 * [word]/[digits]/[itemName] have shared implementations; everything else is
 * implemented per language. [digits] renders integers in the language's own numerals
 * (Bengali shows ০-৯), which feeds the number buttons, equation line and count badges.
 */
interface LangPack {
    val numWords: List<String>          // 0..20
    val digitChars: String              // "0123456789" or "০১২৩৪৫৬৭৮৯"
    val itemNames: Map<String, String>  // emoji -> base noun

    fun word(n: Int): String = numWords[n]

    fun digits(n: Int): String = buildString {
        n.toString().forEach { append(digitChars[it.digitToInt()]) }
    }

    fun itemName(emoji: String): String = itemNames[emoji] ?: "things"

    /** "three apples" / "তিনটি আপেল" (no trailing punctuation). [item] is the noun. */
    fun countPhrase(n: Int, item: String): String

    /** "three apples!" — used as a spoken exclamation. */
    fun countExclaim(n: Int, item: String): String = countPhrase(n, item) + "!"

    /* ---------- narration ---------- */
    fun levelUp(cap: Int): String
    fun voicePreview(): String
    fun slowVoice(): String
    fun normalVoice(): String
    fun howManyLeftPrompt(): String
    fun howManyLeftCount(): String
    fun countTheItems(item: String): String
    fun tapAndCountPrompt(): String
    fun andMore(n: Int): String
    fun tapTheItems(item: String): String
    fun takeAwayPrompt(n: Int): String
    fun takeAwaySpeak(n: Int, item: String): String
    fun takeAwayHint(n: Int, item: String): String
    fun putTogetherPrompt(): String
    fun putTogetherSpeak(a: Int, b: Int): String
    fun tapTheBasket(): String
    fun countAllPrompt(): String
    fun countAllSpeak(): String
    fun plusResult(a: Int, b: Int, r: Int): String
    fun zeroPrompt(): String
    fun minusZeroSpeak(a: Int, b: Int): String
    fun minusResult(a: Int, b: Int, r: Int): String
    fun tapAndCountItem(item: String): String
    fun pickNumber(): String
    fun plusOrMinus(): String
    fun plusWord(): String
    fun minusWord(): String
    fun pickAnother(): String
    fun watchTheItems(item: String): String
    fun morePrompt(b: Int): String
    fun moreComing(b: Int, item: String): String
    fun howManyAltogetherPrompt(): String
    fun howManyAltogether(item: String): String
    fun wentAway(): String
    fun goAwayPrompt(b: Int): String
    fun goAway(b: Int): String
    fun howManyLeft(item: String): String
    fun tapThenAnswer(item: String): String
    fun countTogether(): String
    fun recountAltogether(size: Int): String
    fun recountLeft(size: Int): String
    fun praiseAnswer(n: Int): String
    fun tryCounting(): String

    /* ---------- UI labels ---------- */
    fun appTitle(): String
    fun playBtn(): String
    fun menuPlay(): String
    fun menuFree(): String
    fun menuQuiz(): String
    fun settingsTitle(): String
    fun defaultVoice(): String
    fun slow(): String
    fun normal(): String
    fun resetProgress(): String
    fun done(): String
    fun levelTitle(level: Int): String
    fun nowCountingTo(cap: Int): String
    fun surprise(): String
    fun putTogetherBtn(): String
    fun againBtn(): String
    fun newNumbersBtn(): String
    fun areYouSure(): String
    fun yes(): String
    fun no(): String
    /** Shown in settings when the device has no TTS voice for this language. */
    fun noVoiceNote(): String

    /* ---------- TalkBack content descriptions ---------- */
    fun settingsLabel(): String
    fun muteLabel(): String
    fun unmuteLabel(): String
}

/* ============================================================
   ENGLISH — reproduces the original strings exactly.
   ============================================================ */
object EnPack : LangPack {
    override val numWords = G.NUM_WORDS
    override val digitChars = "0123456789"
    override val itemNames = G.NAMES

    /** Original English-only singular heuristic for the fixed noun set. */
    private fun plural(name: String, n: Int): String = if (n != 1) name else when {
        name.endsWith("ies") -> name.dropLast(3) + "y"   // strawberries -> strawberry
        name.endsWith("oes") -> name.dropLast(2)         // mangoes -> mango
        else -> name.removeSuffix("s")
    }

    override fun countPhrase(n: Int, item: String): String = "${word(n)} ${plural(item, n)}"

    override fun levelUp(cap: Int) = "Level up! Now counting up to ${word(cap)}!"
    override fun voicePreview() = "Hello! Let's count!"
    override fun slowVoice() = "Slow voice."
    override fun normalVoice() = "Normal voice."
    override fun howManyLeftPrompt() = "How many are left?"
    override fun howManyLeftCount() = "How many are left? Count them!"
    override fun countTheItems(item: String) = "Count the $item!"
    override fun tapAndCountPrompt() = "Tap and count!"
    override fun andMore(n: Int) = "And ${word(n)} more! Tap and count!"
    override fun tapTheItems(item: String) = "Tap the $item!"
    override fun takeAwayPrompt(n: Int) = "Take away ${word(n)}!"
    override fun takeAwaySpeak(n: Int, item: String) =
        "Now take away ${word(n)}! Tap ${word(n)} ${plural(item, n)}!"
    override fun takeAwayHint(n: Int, item: String) = "Take away ${word(n)} $item!"
    override fun putTogetherPrompt() = "Put them together!"
    override fun putTogetherSpeak(a: Int, b: Int) =
        "${word(a)} and ${word(b)}. Tap the basket — put them together!"
    override fun tapTheBasket() = "Tap the basket!"
    override fun countAllPrompt() = "Count them all!"
    override fun countAllSpeak() = "All together now! Count them all!"
    override fun plusResult(a: Int, b: Int, r: Int) = "${word(a)} plus ${word(b)} makes ${word(r)}!"
    override fun zeroPrompt() = "Zero! All gone!"
    override fun minusZeroSpeak(a: Int, b: Int) =
        "Zero! All gone! ${word(a)} take away ${word(b)} leaves zero!"
    override fun minusResult(a: Int, b: Int, r: Int) = "${word(a)} take away ${word(b)} leaves ${word(r)}!"
    override fun tapAndCountItem(item: String) = "Tap and count the $item!"
    override fun pickNumber() = "Pick a number!"
    override fun plusOrMinus() = "Plus or take away?"
    override fun plusWord() = "plus"
    override fun minusWord() = "take away"
    override fun pickAnother() = "Pick another number!"
    override fun watchTheItems(item: String) = "Watch the $item!"
    override fun morePrompt(b: Int) = "${digits(b)} more!"
    override fun moreComing(b: Int, item: String) = "${word(b)} more ${plural(item, b)} coming!"
    override fun howManyAltogetherPrompt() = "How many altogether?"
    override fun howManyAltogether(item: String) = "How many $item altogether?"
    override fun wentAway() = "went away"
    override fun goAwayPrompt(b: Int) = "${digits(b)} go away!"
    override fun goAway(b: Int) = "${word(b)} ${if (b == 1) "goes" else "go"} away!"
    override fun howManyLeft(item: String) = "How many $item are left?"
    override fun tapThenAnswer(item: String) = "Tap the $item, then pick the answer!"
    override fun countTogether() = "Let's count together!"
    override fun recountAltogether(size: Int) = "${word(size)}! How many altogether?"
    override fun recountLeft(size: Int) = "${word(size)}! How many are left!"
    override fun praiseAnswer(n: Int): String {
        val praise = listOf("Yes! ", "Great job! ", "Well done! ", "Hooray! ")[G.rand(4)]
        return praise + word(n) + "!"
    }
    override fun tryCounting() = "Hmm, let's try counting!"

    override fun appTitle() = "Count & Play"
    override fun playBtn() = "▶ Play"
    override fun menuPlay() = "Play"
    override fun menuFree() = "Free play"
    override fun menuQuiz() = "Quiz"
    override fun settingsTitle() = "Grown-ups 🔧"
    override fun defaultVoice() = "📱 Default voice"
    override fun slow() = "🐢 Slow"
    override fun normal() = "🚶 Normal"
    override fun resetProgress() = "🗑 Reset progress"
    override fun done() = "✔ Done"
    override fun levelTitle(level: Int) = "Level ${digits(level)}!"
    override fun nowCountingTo(cap: Int) = "Now counting up to ${digits(cap)}!"
    override fun surprise() = "🎲 Surprise me!"
    override fun putTogetherBtn() = "🧺 Put together!"
    override fun againBtn() = "🔁 Again"
    override fun newNumbersBtn() = "🎲 New numbers"
    override fun areYouSure() = "Are you sure?"
    override fun yes() = "✔ Yes"
    override fun no() = "✖ No"
    override fun noVoiceNote() = "No English voice on this device — using the default."

    override fun settingsLabel() = "Settings"
    override fun muteLabel() = "Mute sound"
    override fun unmuteLabel() = "Turn sound on"
}

/* ============================================================
   BENGALI (বাংলা) — kid-friendly, native numerals ০-৯.
   Nouns stay unchanged; the classifier "টি" carries the count.
   ============================================================ */
object BnPack : LangPack {
    override val numWords = listOf(
        "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
        "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোল", "সতেরো", "আঠারো", "উনিশ", "বিশ"
    )
    override val digitChars = "০১২৩৪৫৬৭৮৯"
    override val itemNames = mapOf(
        "🍎" to "আপেল",
        "🍊" to "কমলা",
        "🥭" to "আম",
        "🍌" to "কলা",
        "🍓" to "স্ট্রবেরি",
        "⭐" to "তারা",
        "🎈" to "বেলুন",
        "⚽" to "বল",
        "🚗" to "গাড়ি"
    )

    override fun countPhrase(n: Int, item: String) = "${word(n)}টি ${item}"

    override fun levelUp(cap: Int) = "দারুণ! এবার ${word(cap)} পর্যন্ত গুনবো!"
    override fun voicePreview() = "হ্যালো! চলো গুনি!"
    override fun slowVoice() = "ধীর কন্ঠ"
    override fun normalVoice() = "স্বাভাবিক কন্ঠ"
    override fun howManyLeftPrompt() = "কয়টি বাকি আছে?"
    override fun howManyLeftCount() = "কয়টি বাকি আছে? গুনে দেখো!"
    override fun countTheItems(item: String) = "${item}গুলো গুনো!"
    override fun tapAndCountPrompt() = "ট্যাপ করে গুনো!"
    override fun andMore(n: Int) = "আরও ${word(n)}টি! ট্যাপ করে গুনো!"
    override fun tapTheItems(item: String) = "${item}গুলোতে ট্যাপ করো!"
    override fun takeAwayPrompt(n: Int) = "${word(n)}টি বাদ দাও!"
    override fun takeAwaySpeak(n: Int, item: String) =
        "এবার ${word(n)}টি বাদ দাও! ${word(n)}টি ${item} ট্যাপ করো!"
    override fun takeAwayHint(n: Int, item: String) = "${word(n)}টি ${item} বাদ দাও!"
    override fun putTogetherPrompt() = "একসাথে করো!"
    override fun putTogetherSpeak(a: Int, b: Int) =
        "${word(a)} আর ${word(b)}। ঝুড়িতে ট্যাপ করো — একসাথে করো!"
    override fun tapTheBasket() = "ঝুড়িতে ট্যাপ করো!"
    override fun countAllPrompt() = "সবগুলো গুনো!"
    override fun countAllSpeak() = "এবার সবগুলো একসাথে গুনো!"
    override fun plusResult(a: Int, b: Int, r: Int) = "${word(a)} যোগ ${word(b)} হয় ${word(r)}!"
    override fun zeroPrompt() = "শূন্য! সব শেষ!"
    override fun minusZeroSpeak(a: Int, b: Int) =
        "শূন্য! সব শেষ! ${word(a)} থেকে ${word(b)} নিলে থাকে শূন্য!"
    override fun minusResult(a: Int, b: Int, r: Int) = "${word(a)} থেকে ${word(b)} নিলে থাকে ${word(r)}!"
    override fun tapAndCountItem(item: String) = "${item}গুলোতে ট্যাপ করে গুনো!"
    override fun pickNumber() = "একটি সংখ্যা বেছে নাও!"
    override fun plusOrMinus() = "যোগ নাকি বিয়োগ করবে?"
    override fun plusWord() = "যোগ"
    override fun minusWord() = "বিয়োগ"
    override fun pickAnother() = "আরেকটি সংখ্যা বেছে নাও!"
    override fun watchTheItems(item: String) = "${item}গুলো দেখো!"
    override fun morePrompt(b: Int) = "আরও ${digits(b)}টি!"
    override fun moreComing(b: Int, item: String) = "আরও ${word(b)}টি ${item} আসছে!"
    override fun howManyAltogetherPrompt() = "মোট কয়টি?"
    override fun howManyAltogether(item: String) = "মোট কয়টি ${item}?"
    override fun wentAway() = "চলে গেছে"
    override fun goAwayPrompt(b: Int) = "${digits(b)}টি চলে যায়!"
    override fun goAway(b: Int) = "${word(b)}টি চলে যায়!"
    override fun howManyLeft(item: String) = "কয়টি ${item} বাকি আছে?"
    override fun tapThenAnswer(item: String) = "${item}গুলোতে ট্যাপ করো, তারপর উত্তর বেছে নাও!"
    override fun countTogether() = "চলো একসাথে গুনি!"
    override fun recountAltogether(size: Int) = "${word(size)}! মোট কয়টি?"
    override fun recountLeft(size: Int) = "${word(size)}! কয়টি বাকি আছে?"
    override fun praiseAnswer(n: Int): String {
        val praise = listOf("বাঃ! ", "দারুণ! ", "সাবাস! ", "হুররে! ")[G.rand(4)]
        return praise + word(n) + "!"
    }
    override fun tryCounting() = "হুম, চলো গুনে দেখি!"

    override fun appTitle() = "গুনো আর খেলো"
    override fun playBtn() = "▶ খেলো"
    override fun menuPlay() = "খেলো"
    override fun menuFree() = "ফ্রি প্লে"
    override fun menuQuiz() = "কুইজ"
    override fun settingsTitle() = "বড়দের জন্য 🔧"
    override fun defaultVoice() = "📱 ডিফল্ট স্বর"
    override fun slow() = "🐢 ধীর"
    override fun normal() = "🚶 স্বাভাবিক"
    override fun resetProgress() = "🗑 অগ্রগতি মুছুন"
    override fun done() = "✔ হয়েছে"
    override fun levelTitle(level: Int) = "ধাপ ${digits(level)}!"
    override fun nowCountingTo(cap: Int) = "এবার ${digits(cap)} পর্যন্ত গুনবো!"
    override fun surprise() = "🎲 সারপ্রাইজ করো!"
    override fun putTogetherBtn() = "🧺 একসাথে করো!"
    override fun againBtn() = "🔁 আবার"
    override fun newNumbersBtn() = "🎲 নতুন সংখ্যা"
    override fun areYouSure() = "আপনি কি নিশ্চিত?"
    override fun yes() = "✔ হ্যাঁ"
    override fun no() = "✖ না"
    override fun noVoiceNote() = "এই ডিভাইসে বাংলা স্বর নেই — ডিফল্ট স্বর ব্যবহার করা হচ্ছে।"

    override fun settingsLabel() = "সেটিংস"
    override fun muteLabel() = "শব্দ বন্ধ করো"
    override fun unmuteLabel() = "শব্দ চালু করো"
}
