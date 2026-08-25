package app.maqsadah.count_and_play.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * One light tactile tick, the feel of a key seating. Touch, not sound: the
 * mute switch and the no-music rule govern audio, while this answers the
 * finger directly, so a press lands even in a noisy living room. Uses the
 * platform's own clock-tick feedback, so it asks for no permission at all.
 */
@Composable
fun rememberTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
}
