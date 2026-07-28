package app.maqsadah.count_and_play.copy

import app.maqsadah.count_and_play.core.Line
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

object CopyEn : Copy {
    override val language = Language.EN

    private val words = listOf(
        "zero", "one", "two", "three", "four", "five",
        "six", "seven", "eight", "nine", "ten",
    )

    override fun countWord(n: Int) = words.getOrElse(n) { n.toString() }

    override fun digits(n: Int) = n.toString()

    override fun noun(shape: ShapeKind, count: Int): String {
        val one = when (shape) {
            ShapeKind.APPLE -> "apple"
            ShapeKind.PEAR -> "pear"
            ShapeKind.STAR -> "star"
            ShapeKind.LEAF -> "leaf"
            ShapeKind.BLOCK -> "block"
            ShapeKind.BEAD -> "bead"
            ShapeKind.MELON -> "melon"
            ShapeKind.CARROT -> "carrot"
            ShapeKind.TULIP -> "tulip"
            ShapeKind.BALL -> "ball"
        }
        if (count == 1) return one
        return if (shape == ShapeKind.LEAF) "leaves" else "${one}s"
    }

    // Instructions are fixed phrasings, never varied: a 3-year-old wants the
    // same words every time, because that is how it becomes a routine he can
    // predict and eventually stop needing. Seven words is the ceiling.
    override fun speak(line: Line): String = when (line) {
        is Line.CountWord -> countWord(line.n).replaceFirstChar { it.uppercase() }
        is Line.Cardinal -> "${cap(line.n)} ${noun(line.shape, line.n)}."
        Line.CountThem -> "Count them."
        Line.HowMany -> "How many?"

        Line.PickHowMany -> "How many shall we use?"
        Line.PickHowManyMore -> "And how many more?"
        Line.PickHowManyAway -> "How many shall we take away?"

        is Line.GiveN -> "Put ${countWord(line.n)} ${noun(line.shape, line.n)} in the bowl."
        is Line.GaveIt -> "${cap(line.n)}! You did it."
        Line.LetsCount -> "Let's count them."
        is Line.TooMany -> "That's ${countWord(line.got)}. We wanted ${countWord(line.wanted)}."

        Line.WhichHasMore -> "Which has more?"
        is Line.ThisHasMore -> "This one has ${countWord(line.n)}."

        Line.WhatsUnder -> "How many under the leaf?"
        Line.MakeItHere -> "Make it here."

        is Line.PartsNamed -> "${cap(line.a)}, and ${countWord(line.b)}."
        Line.HowManyAltogether -> "How many will there be?"
        Line.AllTogetherNow -> "All together now."
        // The plain mathematical statement, not "good job". It is better praise
        // than anything evaluative, and it is also just better teaching.
        is Line.MakesTotal ->
            "${cap(line.total)}! ${cap(line.a)} and ${countWord(line.b)} make ${countWord(line.total)}."
        is Line.AndBackAgain -> "And back again. ${cap(line.a)}, and ${countWord(line.b)}."

        is Line.TakeOut -> "Take ${countWord(line.n)} out."
        Line.HowManyLeft -> "How many are left?"
        is Line.WeMade -> "We made ${countWord(line.n)}."
        Line.NothingLeft -> "Nothing left. Zero."

        Line.NudgeGentle -> "Your turn."
        is Line.NudgeModel -> "Watch. ${cap(line.n)}."

        Line.SessionDone -> "That's all for now."
    }

    override fun activityName(skill: Skill): String = when (skill) {
        Skill.COUNT -> "Count them"
        Skill.GIVE_N -> "Put some in"
        Skill.COMPARE -> "Which has more"
        Skill.HIDDEN -> "Under the leaf"
        Skill.JOIN -> "Put together"
        Skill.SEPARATE -> "Take away"
    }

    private fun cap(n: Int) = countWord(n).replaceFirstChar { it.uppercase() }

    override val ui = object : UiText {
        override val play = "Play"
        override val settings = "Settings"
        override val language = "Language"
        override val voice = "Voice"
        override val defaultVoice = "Device default"
        override val slowVoice = "Slower"
        override val normalVoice = "Normal"
        override val sound = "Sound"
        override val soundOn = "On"
        override val soundOff = "Off"
        override val resetProgress = "Start over"
        override val areYouSure = "Are you sure?"
        override val done = "Done"
        override val next = "Next"
        override val chooseShape = "What shall we count?"
        override val grownUps = "For grown-ups"
        override val gateHint = "Press and hold to open"
        override val noVoiceInstalled =
            "No voice for this language is installed on this device. " +
                "The app still works — every number is shown on screen."
        override val sessionOver = "All done!"
        override val playAgain = "Again"
        override val shelfTitle = "What shall we play?"
        override val freePlay = "Free tray"
        override val back = "Back"
        override val whatTheyreLearning = "What they're working on"
        override fun skillLine(skill: String, level: Int) = "$skill · step $level"
    }
}
