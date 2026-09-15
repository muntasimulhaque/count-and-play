package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.host.UiModel

/** Which pane holds the stage. Screens change many times a round; routes rarely. */
internal enum class Route { Home, Count, Add, Take }

internal val Screen.route: Route
    get() = when (this) {
        Screen.Home -> Route.Home
        is Screen.Count -> Route.Count
        is Screen.Add -> Route.Add
        is Screen.Take -> Route.Take
    }

/** Every callback the play panes can raise, bundled so signatures stay calm. */
internal data class Actions(
    val choose: (Skill) -> Unit,
    val tap: (Int) -> Unit,
    val pour: () -> Unit,
    val home: () -> Unit,
    val openSettings: () -> Unit,
)

/**
 * The one composable the host renders: the current stage, then celebration,
 * then the grown-up layers, each above the last. Everything reads from [ui];
 * nothing here owns state. The first-run picker replaces the whole stage
 * rather than covering it, so nothing beneath is composed at all.
 */
@Composable
fun GameScreen(
    ui: UiModel,
    onChoose: (Skill) -> Unit,
    onTapToken: (Int) -> Unit,
    onPour: () -> Unit,
    onHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
) {
    // The ground fills the whole screen; safeDrawingPadding keeps the content
    // clear of the (transient) system bars. When the bars are hidden this
    // padding is zero, so the toy-box still owns every pixel.
    Box(Modifier.fillMaxSize().background(Ground).safeDrawingPadding()) {
        if (ui.firstRun) {
            FirstRunPicker(copy = ui.copy, onSetLanguage = onSetLanguage)
        } else {
            Stage(
                ui = ui,
                actions = Actions(onChoose, onTapToken, onPour, onHome, onOpenSettings),
                onSetLanguage = onSetLanguage,
                onToggleMute = onToggleMute,
                onCloseSettings = onCloseSettings,
            )
        }
    }
}

/** The play routes, the celebration above them, the grown-up sheet above all. */
@Composable
private fun Stage(
    ui: UiModel,
    actions: Actions,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    BackStack(ui, onCloseSettings, actions.home)
    PlayRoutes(ui, actions)
    ui.flash?.let { flash -> FlashOverlay(flash = flash, copy = ui.copy) }
    // Confetti above the fact card's scrim: the paper falls in front of
    // the arithmetic, not dimmed behind it.
    Sparkle(key = ui.confettiKey)
    SettingsLayer(ui, onSetLanguage, onToggleMute, onCloseSettings)
}
