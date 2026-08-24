package app.maqsadah.count_and_play.copy

import kotlinx.collections.immutable.toPersistentList

/** English: short, warm, toddler-directed. */
object EnCopy : Copy {

    /** The old pack's number words, 0..20. */
    private val words = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen", "twenty",
    )

    /** The ten shapes' item names, ported from the old pack. */
    private val items = mapOf(
        "APPLE" to "apple",
        "PEAR" to "pear",
        "STAR" to "star",
        "LEAF" to "leaf",
        "BLOCK" to "block",
        "BEAD" to "bead",
        "MELON" to "melon",
        "CARROT" to "carrot",
        "TULIP" to "tulip",
        "BALL" to "ball",
    )

    override fun numberWord(n: Int): String = words.getOrElse(n) { digits(n) }

    override fun digits(n: Int): String = n.toString()

    override fun itemName(shapeName: String): String = items[shapeName] ?: "things"

    override fun objectLabel(shapeName: String, countOrder: Int): String =
        if (countOrder == 0) itemName(shapeName)
        else "${itemName(shapeName)}, ${numberWord(countOrder)}"

    override fun homeTitle(): String = "What shall we play?"

    override fun tileCount(): String = "Count them"
    override fun tileAdd(): String = "Put together"
    override fun tileTake(): String = "Take away"

    override fun promptCount(): String = "Tap each one and count!"
    override fun promptAdd(): String = "Put them together!"
    override fun promptTake(b: Int): String = "Take away ${numberWord(b)}!"

    override fun promptAll(): String = "Count them all!"
    override fun promptLeft(): String = "How many are left?"

    override fun cardinal(n: Int): String = "${cap(n)}!"

    override fun factAdd(a: Int, b: Int, total: Int): String =
        "${cap(a)} and ${numberWord(b)} make ${numberWord(total)}!"

    override fun factTake(n: Int, b: Int, left: Int): String =
        "${cap(n)} take away ${numberWord(b)} leaves ${numberWord(left)}!"

    override fun languageName(language: Language): String = when (language) {
        Language.EN -> "English"
        Language.BN -> "বাংলা"
    }

    override fun firstRunTitleEn(): String = "Choose your language"
    override fun firstRunTitleBn(): String = "আপনার ভাষা বাছুন"

    override fun voiceMissingNote(): String =
        "This device has no voice installed for the selected language."

    override fun homeLabel(): String = "Home"
    override fun settingsLabel(): String = "Settings"
    override fun closeLabel(): String = "Close"
    override fun soundOnLabel(): String = "Sound on"
    override fun soundOffLabel(): String = "Sound off"
    override fun settingsTitle(): String = "Grown-ups"

    override fun pourReadyState(): String = "Ready"
    override fun pourNotYetState(): String = "Not yet"

    /** The number word with a capital first letter, for sentence starts. */
    private fun cap(n: Int): String = numberWord(n).replaceFirstChar { it.uppercase() }
}
