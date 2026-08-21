package app.maqsadah.count_and_play.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the device has animations switched off system-wide (animator
 * duration scale 0). Celebrations then land instantly: confetti is skipped
 * and springs snap to their end values, so a vestibular-sensitive grown-up
 * holding the phone is never handed a screen full of moving paper.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
