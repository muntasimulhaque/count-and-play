package app.maqsadah.count_and_play.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import app.maqsadah.count_and_play.copy.BnCopy
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.host.UiModel

/** Which pane holds the stage. Screens change many times a round; routes rarely. */
private enum class Route { Home, Count, Add, Take }

private val Screen.route: Route
    get() = when (this) {
        Screen.Home -> Route.Home
        is Screen.Count -> Route.Count
        is Screen.Add -> Route.Add
        is Screen.Take -> Route.Take
    }

/** Every callback the play panes can raise, bundled so signatures stay calm. */
private data class Actions(
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

/**
 * Shelf and games cross-fade through a gentle zoom, so moving between them
 * reads as walking one room, not swapping slides. The content key is the
 * route, so the many state changes inside one game never re-trigger the
 * transition, and each pane keeps the exact screen it was keyed for while
 * it fades. Reduced motion snaps.
 */
@Composable
private fun PlayRoutes(ui: UiModel, actions: Actions) {
    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = ui.screen,
        contentKey = { it.route },
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (
                    fadeIn(tween(durationMillis = 190)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 190))
                    ) togetherWith fadeOut(tween(durationMillis = 130))
            }
        },
        label = "routes",
    ) { screen ->
        Pane(screen, ui.copy, actions)
    }
}

@Composable
private fun Pane(screen: Screen, copy: Copy, actions: Actions) {
    when (screen) {
        Screen.Home -> HomeScreen(copy = copy, onChoose = actions.choose, onOpenSettings = actions.openSettings)
        is Screen.Count -> CountScreen(state = screen.state, copy = copy, onTap = actions.tap, onHome = actions.home)
        is Screen.Add -> AddScreen(
            state = screen.state,
            copy = copy,
            onTap = actions.tap,
            onPour = actions.pour,
            onHome = actions.home,
        )
        is Screen.Take -> TakeScreen(state = screen.state, copy = copy, onTap = actions.tap, onHome = actions.home)
    }
}

/** The grown-up sheet rides up from the bottom edge; the stage dims beneath. */
@Composable
private fun SettingsLayer(
    ui: UiModel,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    AnimatedVisibility(
        visible = ui.settingsOpen,
        enter = if (reducedMotion) {
            EnterTransition.None
        } else {
            slideInVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(durationMillis = 160))
        },
        exit = if (reducedMotion) {
            ExitTransition.None
        } else {
            slideOutVertically(tween(durationMillis = 240, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(durationMillis = 140))
        },
    ) {
        SettingsSheet(
            copy = ui.copy,
            language = languageOf(ui.copy),
            muted = ui.muted,
            voiceAvailable = ui.voiceAvailable,
            voiceReady = ui.voiceReady,
            onSetLanguage = onSetLanguage,
            onToggleMute = onToggleMute,
            onCloseSettings = onCloseSettings,
        )
    }
}

/**
 * [UiModel] carries the words but not which language produced them; the
 * settings sheet needs to highlight the current one, so recover it from the
 * identity of the copy pack.
 */
private fun languageOf(copy: Copy): Language = if (copy is BnCopy) Language.BN else Language.EN

/**
 * The back gesture resolves one level at a time, so a stray swipe from a
 * round lands on the shelf instead of leaving the app: the settings sheet
 * closes first, then the round returns home. On the shelf nothing is
 * enabled, so the system's own exit takes over.
 */
@Composable
private fun BackStack(ui: UiModel, onCloseSettings: () -> Unit, onHome: () -> Unit) {
    BackHandler(enabled = ui.settingsOpen) { onCloseSettings() }
    BackHandler(enabled = !ui.settingsOpen && ui.screen.route != Route.Home) { onHome() }
}
