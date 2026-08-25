package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.host.Flash

/** Settings are read by a grown-up over the child's shoulder. */
private val AdultSize = 16.sp

/**
 * The big numeral moment after a round completes: the arithmetic itself is the
 * praise, so the card shows nothing else. Pops in at 0.6 and springs to full.
 * The card is a polite live region, so a screen reader announces the fact too.
 */
@Composable
fun FlashOverlay(flash: Flash, copy: Copy) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(flash, reducedMotion) {
        if (reducedMotion) {
            scale.snapTo(1f)
        } else {
            scale.snapTo(0.6f)
            scale.animateTo(1f, PopSpring)
        }
    }
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        FactCard(flash, copy, Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
    }
}

/** The pop the fact card lands with. */
private val PopSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
private fun FactCard(flash: Flash, copy: Copy, modifier: Modifier = Modifier) {
    Column(
        modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(Liner, RoundedCornerShape(Corner))
            .border(BorderStroke(OutlineWidth, Yellow), RoundedCornerShape(Corner))
            .padding(horizontal = 36.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (flash) {
            is Flash.Count -> Numeral(copy.digits(flash.n), Ink, SizeFlash)
            is Flash.Add -> FactRow(
                glyphs = digitsOf(flash.a) + digitsOf(flash.b) + digitsOf(flash.total),
            ) { size ->
                Numeral(copy.digits(flash.a), FlashBlue, size)
                Operator("+", size)
                Numeral(copy.digits(flash.b), FlashOrange, size)
                Operator("=", size)
                Numeral(copy.digits(flash.total), FlashGreen, size)
            }
            is Flash.Take -> FactRow(
                glyphs = digitsOf(flash.n) + digitsOf(flash.b) + digitsOf(flash.left),
            ) { size ->
                Numeral(copy.digits(flash.n), FlashBlue, size)
                Operator("\u2212", size)
                Numeral(copy.digits(flash.b), FlashPink, size)
                Operator("=", size)
                Numeral(copy.digits(flash.left), FlashGreen, size)
            }
        }
    }
}

private fun digitsOf(n: Int): Int = n.toString().length

/**
 * The equation row shrinks to fit whatever width the phone has, so even the
 * widest fact at the largest accessibility font scale stays fully on screen.
 */
@Composable
private fun FactRow(glyphs: Int, content: @Composable (TextUnit) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fontScale = LocalDensity.current.fontScale
        // Digit glyph ~0.62em, each operator ~0.75em including its padding.
        val needed = (glyphs * 0.62f + 2 * 0.75f) * SizeFlash.value * fontScale
        val fit = minOf(1f, maxWidth.value / needed)
        Row(verticalAlignment = Alignment.CenterVertically) {
            content((SizeFlash.value * fit).sp)
        }
    }
}

@Composable
private fun Numeral(text: String, color: Color, size: TextUnit) {
    Text(text, color = color, fontSize = size, fontWeight = ToyBlack, fontFamily = ToyFont)
}

@Composable
private fun Operator(text: String, size: TextUnit) {
    Text(
        text,
        Modifier.padding(horizontal = 8.dp),
        color = Ink,
        fontSize = size * 0.6f,
        fontWeight = ToyBlack,
        fontFamily = ToyFont,
    )
}

/** Bottom card with the two grown-up levers: language and sound. */
@Composable
fun SettingsSheet(
    copy: Copy,
    language: Language,
    muted: Boolean,
    voiceAvailable: Boolean,
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    // The scrim swallows outside taps (closing the sheet) so nothing beneath
    // can be reached by accident, by finger or by screen reader.
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.25f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onCloseSettings() },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Liner, RoundedCornerShape(topStart = Corner, topEnd = Corner))
                .clickable(remember { MutableInteractionSource() }, indication = null) { }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                GrabHandle(Modifier.align(Alignment.Center))
                CloseButton(Modifier.align(Alignment.CenterEnd), copy.closeLabel(), onCloseSettings)
            }
            Spacer(Modifier.height(18.dp))
            LanguageRow(copy, language, onSetLanguage)
            if (!voiceAvailable) {
                Spacer(Modifier.height(10.dp))
                Text(
                    copy.voiceMissingNote(),
                    color = Ink.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontFamily = ToyFont,
                )
            }
            Spacer(Modifier.height(14.dp))
            SoundRow(copy, muted, onToggleMute)
        }
    }
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

@Composable
private fun LanguageRow(copy: Copy, language: Language, onSetLanguage: (Language) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LangButton(
            copy.languageName(Language.EN),
            active = language == Language.EN,
            big = false,
            modifier = Modifier.weight(1f),
        ) { onSetLanguage(Language.EN) }
        LangButton(
            copy.languageName(Language.BN),
            active = language == Language.BN,
            big = false,
            modifier = Modifier.weight(1f),
        ) { onSetLanguage(Language.BN) }
    }
}

@Composable
private fun SoundRow(copy: Copy, muted: Boolean, onToggleMute: () -> Unit) {
    val description = if (muted) copy.soundOffLabel() else copy.soundOnLabel()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(BorderStroke(2.dp, Ink.copy(alpha = 0.2f)), RoundedCornerShape(18.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onToggleMute() }
            .semantics {
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
        // A second, colour-only statement of the state: green when sound flows.
        Box(Modifier.size(14.dp).background(if (muted) Pink else Green, CircleShape))
    }
}

/** The one-time door: nothing is playable until a language has been chosen. */
@Composable
fun FirstRunPicker(copy: Copy, onSetLanguage: (Language) -> Unit) {
    // Opaque and tap-swallowing: no touch reaches the shelf beneath it.
    Box(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .background(Liner, RoundedCornerShape(Corner))
                .border(BorderStroke(OutlineWidth, Blue), RoundedCornerShape(Corner))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(copy.firstRunTitleEn(), color = Ink, fontSize = SizeLabel, fontWeight = ToyBlack, fontFamily = ToyFont)
            Text(copy.firstRunTitleBn(), color = Ink, fontSize = SizeLabel, fontWeight = ToyBlack, fontFamily = ToyFont)
            Spacer(Modifier.height(4.dp))
            LangButton(copy.languageName(Language.EN), active = false, big = true, modifier = Modifier.fillMaxWidth()) {
                onSetLanguage(Language.EN)
            }
            LangButton(copy.languageName(Language.BN), active = false, big = true, modifier = Modifier.fillMaxWidth()) {
                onSetLanguage(Language.BN)
            }
        }
    }
}

@Composable
private fun LangButton(name: String, active: Boolean, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            // A floor, not a ceiling: the box grows with the text under the
            // capped font scale, so no label ever clips at any setting.
            .heightIn(min = if (big) 72.dp else 56.dp)
            .background(if (active) Blue.copy(alpha = 0.18f) else Color.White, RoundedCornerShape(18.dp))
            .border(
                BorderStroke(if (active) 4.dp else 2.dp, if (active) Blue else Ink.copy(alpha = 0.2f)),
                RoundedCornerShape(18.dp),
            )
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics {
                role = Role.Button
                selected = active
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (active) {
                TickMark(Blue, 16.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text(
                name,
                color = Ink,
                fontSize = if (big) SizePrompt else AdultSize,
                fontWeight = ToyBold,
                fontFamily = ToyFont,
            )
        }
    }
}

/** A small checkmark: the active choice is marked twice, in colour and shape. */
@Composable
private fun TickMark(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.16f
        drawLine(color, Offset(w * 0.12f, h * 0.55f), Offset(w * 0.38f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.85f), Offset(w * 0.88f, h * 0.15f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun CloseButton(modifier: Modifier, description: String, onClose: () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            drawLine(Ink, Offset.Zero, Offset(size.width, size.height), 4.dp.toPx(), StrokeCap.Round)
            drawLine(Ink, Offset(size.width, 0f), Offset(0f, size.height), 4.dp.toPx(), StrokeCap.Round)
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
            drawLine(Pink, Offset(w * 0.60f, h * 0.30f), Offset(w * 0.95f, h * 0.70f), w * 0.10f, StrokeCap.Round)
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
