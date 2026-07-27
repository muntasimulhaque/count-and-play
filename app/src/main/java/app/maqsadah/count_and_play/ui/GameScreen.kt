package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.copy.copyFor
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Step
import app.maqsadah.count_and_play.core.Zone
import app.maqsadah.count_and_play.host.Fx
import app.maqsadah.count_and_play.host.GameUiState
import app.maqsadah.count_and_play.host.Screen

/** Everything the child touches, driven by one immutable state. */
@Composable
fun GameScreen(
    state: GameUiState,
    onLanguage: (Language) -> Unit,
    onShape: (ShapeKind) -> Unit,
    onTapToken: (Int) -> Unit,
    onTapZone: (Zone) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    onPlayAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetSlow: (Boolean) -> Unit,
    onSetLanguage: (Language) -> Unit,
    onAskReset: () -> Unit,
    onConfirmReset: () -> Unit,
) {
    val palette = LocalPalette.current
    val copy = copyFor(state.settings.language)

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.tabletop)
            .systemBarsPadding(),
    ) {
        when (state.screen) {
            Screen.LANGUAGE -> LanguagePicker(onLanguage)
            Screen.SHAPE -> ShapePicker(copy, onShape)
            Screen.DONE -> SessionEnd(copy, onPlayAgain)
            Screen.PLAY -> PlayScreen(state, copy, onTapToken, onTapZone, onDone, onNext)
        }

        if (state.screen != Screen.LANGUAGE) {
            GrownUpsGate(
                copy = copy,
                // Top-left, away from everything the child reaches for.
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                onOpen = onOpenSettings,
            )
        }

        if (state.settingsOpen) {
            SettingsOverlay(
                state = state,
                copy = copy,
                onClose = onCloseSettings,
                onSetSound = onSetSound,
                onSetSlow = onSetSlow,
                onSetLanguage = onSetLanguage,
                onAskReset = onAskReset,
                onConfirmReset = onConfirmReset,
            )
        }
    }
}

@Composable
private fun PlayScreen(
    state: GameUiState,
    copy: Copy,
    onTapToken: (Int) -> Unit,
    onTapZone: (Zone) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
) {
    val lesson = state.lesson ?: return
    Column(Modifier.fillMaxSize()) {
        Stage(
            state = lesson,
            fx = state.fx,
            copy = copy,
            modifier = Modifier.weight(1f),
            onTapToken = onTapToken,
            onTapZone = onTapZone,
        )

        // Nothing advances by itself. The child says when he is finished, and
        // when he is ready for the next one — there is no autoplay in this app.
        when (lesson.step) {
            Step.Giving, Step.Taking, Step.Predicting ->
                BigButton(copy.ui.done, Modifier.padding(16.dp).fillMaxWidth(), onDone)
            Step.Finished ->
                BigButton(copy.ui.next, Modifier.padding(16.dp).fillMaxWidth(), onNext)
            else -> Box(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun BigButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier
            .height(88.dp)
            .background(palette.tray, RoundedCornerShape(24.dp))
            .border(4.dp, palette.trayRim, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = palette.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LanguagePicker(onPick: (Language) -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Count & Play", color = palette.ink, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.height(32.dp))
        for (language in Language.entries) {
            BigButton(language.nativeName, Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                onPick(language)
            }
        }
    }
}

/**
 * The app's opening act is a real choice, and the most motivating one available
 * to a 3-year-old: what shall we count? The old build gave it to a random
 * number generator and offered the child a menu of adult concepts instead.
 */
@Composable
private fun ShapePicker(copy: Copy, onPick: (ShapeKind) -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(copy.ui.chooseShape, color = palette.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.height(20.dp))
        ShapeKind.all.chunked(4).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { shape -> ShapeChoice(shape, copy) { onPick(shape) } }
            }
        }
    }
}

@Composable
private fun ShapeChoice(shape: ShapeKind, copy: Copy, onPick: () -> Unit) {
    val palette = LocalPalette.current
    val colors = colorsFor(shape)
    Box(
        Modifier
            .size(84.dp)
            .background(palette.trayLiner, RoundedCornerShape(20.dp))
            .border(3.dp, palette.trayRim, RoundedCornerShape(20.dp))
            .clickable(onClick = onPick)
            .semantics { contentDescription = copy.noun(shape, 1) }
            .drawBehind {
                val cell = size.minDimension * 0.74f
                translate((size.width - cell) / 2f, (size.height - cell) / 2f) {
                    drawCountable(shape, cell, colors, palette, Detail.FULL)
                }
            },
    )
}

@Composable
private fun SessionEnd(copy: Copy, onAgain: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(copy.ui.sessionOver, color = palette.ink, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.height(28.dp))
        BigButton(copy.ui.playAgain, Modifier.fillMaxWidth(), onAgain)
    }
}

/**
 * A press-and-hold gate.
 *
 * Deliberately not a tap: the old build opened grown-ups settings on a single
 * tap, so a toddler poking the screen could reach a reset-progress button. A
 * sustained two-second hold on a small target is something 3-year-olds rarely
 * produce on purpose, and adults find trivial.
 */
@Composable
private fun GrownUpsGate(copy: Copy, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier
            .size(56.dp)
            .semantics { contentDescription = "${copy.ui.grownUps}. ${copy.ui.gateHint}" }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpen() })
            }
            .drawBehind {
                val r = size.minDimension * 0.28f
                drawCircle(palette.inkSoft, r, Offset(size.width / 2f, size.height / 2f), style = Stroke(width = r * 0.34f))
                drawCircle(palette.inkSoft, r * 0.30f, Offset(size.width / 2f, size.height / 2f))
            },
    )
}
