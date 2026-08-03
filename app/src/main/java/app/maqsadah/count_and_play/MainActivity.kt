package app.maqsadah.count_and_play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.maqsadah.count_and_play.host.GameHost
import app.maqsadah.count_and_play.ui.GameScreen

class MainActivity : ComponentActivity() {

    private val host: GameHost by viewModels { GameHost.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            // A backgrounded app stops talking; the round resumes where it
            // stopped when the child returns.
            override fun onStart(owner: LifecycleOwner) = host.resume()
            override fun onStop(owner: LifecycleOwner) = host.pause()
        })
        setContent {
            val ui by host.ui.collectAsState()
            GameScreen(
                ui = ui,
                onChoose = host::choose,
                onTapToken = host::tap,
                onHome = host::home,
                onOpenSettings = host::openSettings,
                onCloseSettings = host::closeSettings,
                onSetLanguage = host::setLanguage,
                onToggleMute = host::toggleMute,
            )
        }
    }
}
