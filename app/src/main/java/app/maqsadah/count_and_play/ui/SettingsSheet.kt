package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
        // The scrim answers a tap anywhere outside the sheet. It is a plain
        // pointer target on purpose: making it a button would hand a screen
        // reader an unlabelled full-screen control. The sheet's own Close is
        // the one named way out.
        Box(
            Modifier.matchParentSize().pointerInput(onCloseSettings) {
                detectTapGestures { onCloseSettings() }
            },
        )
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

/** The gap between the two language choices. */
private val LangGap = 14.dp

@Composable
private fun LanguageRow(copy: Copy, language: Language, onSetLanguage: (Language) -> Unit) {
    val reducedMotion = rememberReducedMotion()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val half = ((maxWidth - LangGap) / 2).coerceAtLeast(0.dp)
        val glide by animateDpAsState(
            targetValue = if (language == Language.BN) half + LangGap else 0.dp,
            animationSpec = if (reducedMotion) snap() else tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "langPill",
        )
        // The gliding pill: one selection surface travels between the two
        // choices, so switching reads as one thing moving, not two changing.
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset { IntOffset(x = glide.roundToPx(), y = 0) }
                    .width(half)
                    .fillMaxHeight()
                    .background(Blue.copy(alpha = 0.12f), RoundedCornerShape(CornerSmall))
                    .border(BorderStroke(2.dp, Blue), RoundedCornerShape(CornerSmall)),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LangGap)) {
            LangChoice(copy.languageName(Language.EN), active = language == Language.EN, modifier = Modifier.weight(1f)) {
                onSetLanguage(Language.EN)
            }
            LangChoice(copy.languageName(Language.BN), active = language == Language.BN, modifier = Modifier.weight(1f)) {
                onSetLanguage(Language.BN)
            }
        }
    }
}

/** One language choice: label and tick only, the travelling pill carries the surface. */
@Composable
private fun LangChoice(name: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 56.dp)
            .pressable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = active
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (active) {
            TickMark(Blue, 16.dp)
            Spacer(Modifier.size(8.dp))
        }
        Text(name, color = Ink, fontSize = AdultSize, fontWeight = ToyBold, fontFamily = ToyFont)
    }
}

@Composable
private fun SoundRow(copy: Copy, muted: Boolean, onToggleMute: () -> Unit) {
    val description = if (muted) copy.soundOffLabel() else copy.soundOnLabel()
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onToggleMute)
            .background(Liner, RoundedCornerShape(CornerSmall))
            .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(CornerSmall))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeakerIcon(muted, 26.dp, Ink)
        Text(
            description,
            Modifier.padding(start = 12.dp).weight(1f),
            color = Ink,
            fontSize = AdultSize,
            fontWeight = ToyBold,
            fontFamily = ToyFont,
        )
        // A second, colour-only statement of the state: green when sound
        // flows, red when it is switched off.
        Box(Modifier.size(14.dp).background(if (muted) Red else Green, CircleShape))
    }
}

@Composable
private fun CloseButton(modifier: Modifier, description: String, onClose: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .pressable(onClick = onClose)
            .background(Ink.copy(alpha = 0.05f), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 3.5.dp.toPx()
            drawLine(Ink.copy(alpha = 0.75f), Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(Ink.copy(alpha = 0.75f), Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}

/** A speaker with sound waves; when muted, the waves become a single slash. */
@Composable
private fun SpeakerIcon(muted: Boolean, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val body = Path().apply {
            moveTo(w * 0.05f, h * 0.34f)
            lineTo(w * 0.28f, h * 0.34f)
            lineTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.28f, h * 0.66f)
            lineTo(w * 0.05f, h * 0.66f)
            close()
        }
        drawPath(body, color)
        if (muted) {
            drawLine(Red, Offset(w * 0.60f, h * 0.30f), Offset(w * 0.95f, h * 0.70f), w * 0.10f, StrokeCap.Round)
        } else {
            drawArc(
                color, -50f, 100f, false,
                topLeft = Offset(w * 0.55f, h * 0.30f),
                size = Size(w * 0.34f, h * 0.40f),
                style = Stroke(w * 0.09f, cap = StrokeCap.Round),
            )
            drawArc(
                color, -50f, 100f, false,
                topLeft = Offset(w * 0.62f, h * 0.16f),
                size = Size(w * 0.56f, h * 0.68f),
                style = Stroke(w * 0.09f, cap = StrokeCap.Round),
            )
        }
    }
}
