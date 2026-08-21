package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.maqsadah.count_and_play.copy.BnCopy
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.host.UiModel

/**
 * The one composable the host renders: the current screen, then celebration,
 * then the grown-up layers, each above the last. Everything reads from [ui];
 * nothing here owns state. The first-run picker replaces the whole shelf
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
            when (val screen = ui.screen) {
                Screen.Home -> HomeScreen(copy = ui.copy, onChoose = onChoose, onOpenSettings = onOpenSettings)
                is Screen.Count -> CountScreen(state = screen.state, copy = ui.copy, onTap = onTapToken, onHome = onHome)
                is Screen.Add -> AddScreen(state = screen.state, copy = ui.copy, onTap = onTapToken, onPour = onPour, onHome = onHome)
                is Screen.Take -> TakeScreen(state = screen.state, copy = ui.copy, onTap = onTapToken, onHome = onHome)
            }
            ui.flash?.let { flash -> FlashOverlay(flash = flash, copy = ui.copy) }
            // Confetti above the fact card's scrim: the paper falls in front of
            // the arithmetic, not dimmed behind it.
            Sparkle(key = ui.confettiKey)
            if (ui.settingsOpen) {
                SettingsSheet(
                    copy = ui.copy,
                    language = languageOf(ui.copy),
                    muted = ui.muted,
                    voiceAvailable = ui.voiceAvailable,
                    onSetLanguage = onSetLanguage,
                    onToggleMute = onToggleMute,
                    onCloseSettings = onCloseSettings,
                )
            }
        }
    }
}

/**
 * [UiModel] carries the words but not which language produced them; the
 * settings sheet needs to highlight the current one, so recover it from the
 * identity of the copy pack.
 */
private fun languageOf(copy: Copy): Language = if (copy is BnCopy) Language.BN else Language.EN
