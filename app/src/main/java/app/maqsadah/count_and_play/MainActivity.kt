package app.maqsadah.count_and_play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.maqsadah.count_and_play.host.GameHost
import app.maqsadah.count_and_play.ui.GameScreen

/** The most the toy-box lets system font scaling grow its words. */
private const val MAX_FONT_SCALE = 1.3f

class MainActivity : ComponentActivity() {

    private val host: GameHost by viewModels { GameHost.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A child's toy-box owns the whole screen. Edge-to-edge is enforced at
        // this targetSdk, so without some handling the game drew under the
        // status and navigation bars. We hide both bars for an immersive,
        // distraction-free play surface; they only flash back transiently on a
        // swipe, and the UI's safe-insets padding keeps content clear of them.
        keepBarsHidden()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            // A backgrounded app stops talking; on return the host re-performs
            // whatever beats the interruption cut short.
            override fun onStart(owner: LifecycleOwner) {
                // The system may re-show the bars while the app is away.
                keepBarsHidden()
                host.resume()
            }
            override fun onStop(owner: LifecycleOwner) = host.pause()
        })
        setContent {
            // A toy-box, not a document: text follows the system font setting,
            // but only so far. Past this cap the words stop fitting the fixed
            // play surfaces and begin to overlap them, which serves nobody, so
            // the whole UI is composed under a bounded density instead.
            val system = LocalDensity.current
            val capped = remember(system.fontScale) {
                Density(density = system.density, fontScale = minOf(system.fontScale, MAX_FONT_SCALE))
            }
            CompositionLocalProvider(LocalDensity provides capped) {
                val ui by host.ui.collectAsStateWithLifecycle()
                GameScreen(
                    ui = ui,
                    onChoose = host::choose,
                    onTapToken = host::tap,
                    onPour = host::pour,
                    onHome = host::home,
                    onOpenSettings = host::openSettings,
                    onCloseSettings = host::closeSettings,
                    onSetLanguage = host::setLanguage,
                    onToggleMute = host::toggleMute,
                )
            }
        }
    }

    /** Full-screen immersive: hide the system bars, show them only on a swipe. */
    private fun keepBarsHidden() {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}
