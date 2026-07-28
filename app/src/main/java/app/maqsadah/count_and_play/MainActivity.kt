package app.maqsadah.count_and_play

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.maqsadah.count_and_play.host.GameHost
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.ui.CountPlayTheme
import app.maqsadah.count_and_play.ui.GameScreen

class MainActivity : ComponentActivity() {

    private val host: GameHost by viewModels { GameHost.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A 3-year-old can look at a set for a long time without touching it,
        // and the screen going dark mid-thought is its own small failure.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Backgrounding stops the app dead rather than leaving it narrating
            // and completing rounds unseen, as the old build did.
            override fun onStop(owner: LifecycleOwner) = host.onEnterBackground()
            override fun onStart(owner: LifecycleOwner) = host.onReturnToForeground()
        })

        setContent {
            CountPlayTheme {
                val state = host.ui

                // Back mid-round returns to the shelf rather than quitting: a
                // toddler presses Back constantly, and losing the round to a
                // stray press is indistinguishable from a crash. Pressing it
                // again from the shelf leaves normally.
                val inActivity = state.screen == Screen.PLAY ||
                    state.screen == Screen.PICK ||
                    state.screen == Screen.FREE
                BackHandler(enabled = inActivity && !state.settingsOpen) {
                    host.goHome()
                }

                GameScreen(
                    state = state,
                    onLanguage = host::chooseLanguage,
                    onShape = host::chooseShape,
                    onChangeShape = host::changeShape,
                    onStartSkill = host::startSkill,
                    onFreePlay = host::startFreePlay,
                    onPickNumber = host::pickNumber,
                    onTapFree = host::tapFree,
                    onHome = host::goHome,
                    onTapToken = host::tapToken,
                    onTapZone = host::tapZone,
                    onDone = host::done,
                    onNext = host::next,
                    onOpenSettings = host::openSettings,
                    onCloseSettings = host::closeSettings,
                    onSetSound = host::setSound,
                    onSetSlow = host::setSlowRate,
                    onSetLanguage = host::setLanguage,
                    onAskReset = host::askReset,
                    onConfirmReset = host::confirmReset,
                )
            }
        }
    }
}
