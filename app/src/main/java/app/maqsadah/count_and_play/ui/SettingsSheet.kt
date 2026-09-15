package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language

/** The sheet's rounded top, the only radius it has. */
private val SheetTop = RoundedCornerShape(topStart = Corner, topEnd = Corner)

/**
 * The grown-up corner: language and sound, on a white sheet that rides up
 * from the bottom edge (the ride itself lives in the settings layer above
 * the stage). The scrim swallows outside taps so nothing beneath can be
 * reached by accident, by finger or by screen reader.
 */
@Composable
fun SettingsSheet(
    copy: Copy,
    language: Language,
    muted: Boolean,
    voiceAvailable: Boolean,
    voiceReady: Boolean,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.25f)),
    ) {
        SheetScrim(onCloseSettings)
        SheetPanel(copy, language, muted, voiceAvailable, voiceReady, onSetLanguage, onToggleMute, onCloseSettings)
    }
}

/**
 * The scrim answers a tap anywhere outside the sheet. It is a plain pointer
 * target on purpose: making it a button would hand a screen reader an
 * unlabeled full-screen control. The sheet's own Close is the one named way
 * out.
 */
@Composable
private fun BoxScope.SheetScrim(onCloseSettings: () -> Unit) {
    Box(
        Modifier.matchParentSize().pointerInput(onCloseSettings) {
            detectTapGestures { onCloseSettings() }
        },
    )
}

/** The sheet itself, holding the grown-up rows and swallowing their taps. */
@Composable
private fun BoxScope.SheetPanel(
    copy: Copy,
    language: Language,
    muted: Boolean,
    voiceAvailable: Boolean,
    voiceReady: Boolean,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .shadow(elevation = LiftRaised, shape = SheetTop, clip = false)
            .clip(SheetTop)
            .background(Liner)
            // Taps that land on the sheet must not reach the scrim behind.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetHeader(copy.closeLabel(), onCloseSettings)
        Spacer(Modifier.height(18.dp))
        LanguageRow(copy, language, onSetLanguage)
        // Only a checked device may be told it lacks a voice: a cold
        // engine that has not bound yet is not a missing voice.
        if (voiceReady && !voiceAvailable) VoiceNote(copy)
        Spacer(Modifier.height(14.dp))
        SoundRow(copy, muted, onToggleMute)
    }
}

@Composable
private fun SheetHeader(closeLabel: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        GrabHandle(Modifier.align(Alignment.Center))
        CloseButton(Modifier.align(Alignment.CenterEnd), closeLabel, onClose)
    }
}

@Composable
private fun VoiceNote(copy: Copy) {
    Spacer(Modifier.height(10.dp))
    Text(
        copy.voiceMissingNote(),
        color = Ink.copy(alpha = 0.7f),
        fontSize = 14.sp,
        fontFamily = ToyFont,
    )
}

/** The sheet's grab handle: a quiet bar that says this is a panel, not the app. */
@Composable
private fun GrabHandle(modifier: Modifier) {
    Box(
        modifier
            .width(44.dp)
            .height(5.dp)
            .background(Ink.copy(alpha = 0.22f), RoundedCornerShape(3.dp)),
    )
}
