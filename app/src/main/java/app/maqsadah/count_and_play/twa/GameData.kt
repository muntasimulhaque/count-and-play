package app.maqsadah.count_and_play.twa

import androidx.compose.ui.graphics.Color

/** Shared game data — a direct port of the constants in the original web app. */
object G {
    val ITEMS = listOf("🍎", "🍊", "🥭", "🍌", "🍓", "⭐", "🎈", "⚽", "🚗")
    val NAMES = mapOf(
        "🍎" to "apples",
        "🍊" to "oranges",
        "🥭" to "mangoes",
        "🍌" to "bananas",
        "🍓" to "strawberries",
        "⭐" to "stars",
        "🎈" to "balloons",
        "⚽" to "balls",
        "🚗" to "cars"
    )
    val NUM_WORDS = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
    )
    const val MAX_N = 20

    fun rand(n: Int): Int = (0 until n).random()
    fun word(n: Int): String = NUM_WORDS[n]
    fun plural(name: String, n: Int): String = if (n != 1) name else when {
        name.endsWith("ies") -> name.dropLast(3) + "y"   // strawberries -> strawberry
        name.endsWith("oes") -> name.dropLast(2)         // mangoes -> mango
        else -> name.removeSuffix("s")
    }
}

/** Colors ported from the original CSS. */
object Palette {
    val TextBlue = Color(0xFF2A5D8F)
    val Q = Color(0xFFE67E22)
    val Ghost = Color(0xFFB8CFE0)
    val SkyTop = Color(0xFFAEE7FF)
    val SkyMid = Color(0xFFE8F9FF)
    val Mint = Color(0xFFD4F5D4)
    val OverlayTop = Color(0xFF7EC8E3)
    val OverlayBottom = Color(0xFFA8E6CF)
    val PlateABorder = Color(0xFF8ECAE6)
    val PlateBBorder = Color(0xFFF4A261)
    val GoneBorder = Color(0xFFBBBBBB)
    val SlotBorder = Color(0xFFE07A5F)
    val ZoneBg = Color(0x8CFFFFFF)          // white 55%
    val GoneBg = Color(0x40C8C8C8)          // grey 25%
    val BtnShadow = Color(0xFFC9DCE8)
    val Green = Color(0xFF7DDF7D)
    val GreenShadow = Color(0xFF56B856)
    val OrangeBtn = Color(0xFFF4A261)
    val OrangeBtnShadow = Color(0xFFD07F3F)
    val Blue = Color(0xFF8AB6F9)
    val BlueShadow = Color(0xFF5A8FE0)
    val Purple = Color(0xFFC8A2FF)
    val PurpleShadow = Color(0xFFA276E0)
    val Yellow = Color(0xFFFFB703)
    val YellowShadow = Color(0xFFD99502)
    val PromptGrey = Color(0xFF444444)
    val LabelGrey = Color(0xFF999999)
    val WrongPink = Color(0xFFFFD6D6)
}
