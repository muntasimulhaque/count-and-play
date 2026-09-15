package app.maqsadah.count_and_play.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.host.UiModel

/** The grown-up sheet rides up from the bottom edge; the stage dims beneath. */
@Composable
internal fun SettingsLayer(
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
            language = ui.language,
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
 * The back gesture resolves one level at a time, so a stray swipe from a
 * round lands on the shelf instead of leaving the app: the settings sheet
 * closes first, then the round returns home. On the shelf nothing is
 * enabled, so the system's own exit takes over.
 */
@Composable
internal fun BackStack(ui: UiModel, onCloseSettings: () -> Unit, onHome: () -> Unit) {
    BackHandler(enabled = ui.settingsOpen) { onCloseSettings() }
    BackHandler(enabled = !ui.settingsOpen && ui.screen.route != Route.Home) { onHome() }
}
