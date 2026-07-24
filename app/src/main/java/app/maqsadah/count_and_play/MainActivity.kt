package app.maqsadah.count_and_play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {

    // The ViewModel owns the game state/sequencing (viewModelScope) and the
    // Speaker (built from the Application context, released in onCleared).
    private val vm: GameViewModel by viewModels { GameViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen, like the original app
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        insets.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())

        // Silence narration the moment the app leaves the screen so the TTS
        // voice never trails on after the child switches away; resume it on
        // return. Game state lives in the ViewModel, so the round picks back
        // up exactly where it stopped. (onStop = fully backgrounded.)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = vm.speaker.resume()
            override fun onStop(owner: LifecycleOwner) = vm.speaker.pause()
        })

        setContent {
            CountPlayApp(vm, vm.speaker)
        }
    }
}
