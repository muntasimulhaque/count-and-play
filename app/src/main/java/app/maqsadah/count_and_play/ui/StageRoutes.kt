package app.maqsadah.count_and_play.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.host.Screen
import app.maqsadah.count_and_play.host.UiModel

/**
 * Shelf and games cross-fade through a gentle zoom, so moving between them
 * reads as walking one room, not swapping slides. The content key is the
 * route, so the many state changes inside one game never re-trigger the
 * transition, and each pane keeps the exact screen it was keyed for while
 * it fades. Reduced motion snaps.
 *
 * While the fact card is up the whole stage settles back: the card is the
 * mathematics arriving, and nothing should compete with it for the eye.
 */
@Composable
internal fun PlayRoutes(ui: UiModel, actions: Actions) {
    val reducedMotion = rememberReducedMotion()
    val stageAlpha by animateFloatAsState(
        targetValue = if (ui.flash != null) 0.45f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 200),
        label = "stageAlpha",
    )
    AnimatedContent(
        targetState = ui.screen,
        contentKey = { it.route },
        modifier = Modifier.graphicsLayer { alpha = stageAlpha },
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
