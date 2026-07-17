package app.maqsadah.count_and_play.twa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private var speaker: Speaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sp = Speaker(this)
        speaker = sp
        val controller = GameController(sp, lifecycleScope)

        // Full-screen, like the original app
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        insets.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            CountPlayApp(controller, sp)
        }
    }

    override fun onDestroy() {
        speaker?.shutdown()
        super.onDestroy()
    }
}
