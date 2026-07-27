package app.maqsadah.count_and_play.host

import app.maqsadah.count_and_play.core.LessonState
import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.data.Settings

enum class Screen {
    /** Shown once, before anything else, if no language has been chosen. */
    LANGUAGE,

    /** The child picks what to count. The app's opening act is a real choice. */
    SHAPE,

    PLAY,

    /** A calm, explicit end. There is no autoplay anywhere in this app. */
    DONE,
}

/**
 * Transient visual state — things that are true of the picture but not of the
 * rules. Kept out of [LessonState] so the domain stays free of decoration.
 */
data class Fx(
    /** Tokens that have finished arriving. Nothing renders before it lands. */
    val revealed: Set<Int> = emptySet(),

    /** A zone's tags have collapsed into one number, shown on its rim. */
    val cardinals: Map<Zone, Int> = emptyMap(),

    val covered: Set<Zone> = emptySet(),
    val highlighted: Set<Int> = emptySet(),
    val predicting: Boolean = false,
    val celebrating: Boolean = false,
)

/**
 * Everything the screen needs, as one plain data class.
 *
 * No composable in this app takes a ViewModel. That is what lets the screenshot
 * tests render any state directly — including states that are hard to reach by
 * playing — with no TTS, no timing and no emulator flakiness.
 */
data class GameUiState(
    val screen: Screen = Screen.LANGUAGE,
    val settings: Settings = Settings(),
    val progress: Progress = Progress(),
    val lesson: LessonState? = null,
    val fx: Fx = Fx(),
    val taskIndex: Int = 0,
    val tasksInSession: Int = 0,
    val settingsOpen: Boolean = false,
    val confirmingReset: Boolean = false,
    /** True when the chosen language has no installed voice. The app still
     *  teaches — every number is on screen — but the grown-up is told. */
    val voiceMissing: Boolean = false,
)
