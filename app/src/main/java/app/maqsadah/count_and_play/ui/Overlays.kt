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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
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
 */
@Composable
fun FlashOverlay(flash: Flash, copy: Copy) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(flash) {
        scale.snapTo(0.6f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .background(Liner, RoundedCornerShape(Corner))
                .border(BorderStroke(OutlineWidth, Yellow), RoundedCornerShape(Corner))
                .padding(horizontal = 36.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (flash) {
                is Flash.Count -> {
                    Numeral(copy.digits(flash.n), Ink)
                    Text(
                        copy.celebrate(),
                        color = Ink,
                        fontSize = SizeLabel,
                        fontWeight = ToyBold,
                        fontFamily = ToyFont,
                    )
                }
                is Flash.Add -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Numeral(copy.digits(flash.a), Blue)
                    Operator("+")
                    Numeral(copy.digits(flash.b), Orange)
                    Operator("=")
                    Numeral(copy.digits(flash.total), Green)
                }
                is Flash.Take -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Numeral(copy.digits(flash.n), Blue)
                    Operator("\u2212")
                    Numeral(copy.digits(flash.b), Pink)
                    Operator("=")
                    Numeral(copy.digits(flash.left), Green)
                }
            }
        }
    }
}

@Composable
private fun Numeral(text: String, color: Color) {
    Text(text, color = color, fontSize = SizeFlash, fontWeight = ToyBlack, fontFamily = ToyFont)
}

@Composable
private fun Operator(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 8.dp),
        color = Ink,
        fontSize = SizeFlash * 0.6f,
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
    onSetLanguage: (Language) -> Unit,
    onToggleMute: () -> Unit,
    onCloseSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.25f))) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Liner, RoundedCornerShape(topStart = Corner, topEnd = Corner))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                CloseButton(Modifier.align(Alignment.CenterEnd), onCloseSettings)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LangButton("English", active = language == Language.EN, big = false, modifier = Modifier.weight(1f)) {
                    onSetLanguage(Language.EN)
                }
                LangButton("বাংলা", active = language == Language.BN, big = false, modifier = Modifier.weight(1f)) {
                    onSetLanguage(Language.BN)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onToggleMute() }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeakerIcon(muted, 32.dp, Ink)
            }
        }
    }
}

/** The one-time door: nothing is playable until a language has been chosen. */
@Composable
fun FirstRunPicker(onSetLanguage: (Language) -> Unit) {
    Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
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
            Text("Choose your language", color = Ink, fontSize = SizeLabel, fontWeight = ToyBlack, fontFamily = ToyFont)
            Text("আপনার ভাষা বাছুন", color = Ink, fontSize = SizeLabel, fontWeight = ToyBlack, fontFamily = ToyFont)
            Spacer(Modifier.height(4.dp))
            LangButton("English", active = false, big = true, modifier = Modifier.fillMaxWidth()) {
                onSetLanguage(Language.EN)
            }
            LangButton("বাংলা", active = false, big = true, modifier = Modifier.fillMaxWidth()) {
                onSetLanguage(Language.BN)
            }
        }
    }
}

@Composable
private fun LangButton(name: String, active: Boolean, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(if (big) 72.dp else 56.dp)
            .background(if (active) Blue.copy(alpha = 0.18f) else Color.White, RoundedCornerShape(18.dp))
            .border(
                BorderStroke(if (active) 4.dp else 2.dp, if (active) Blue else Ink.copy(alpha = 0.2f)),
                RoundedCornerShape(18.dp),
            )
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name,
            color = Ink,
            fontSize = if (big) SizePrompt else AdultSize,
            fontWeight = ToyBold,
            fontFamily = ToyFont,
        )
    }
}

@Composable
private fun CloseButton(modifier: Modifier, onClose: () -> Unit) {
    Box(
        modifier.size(48.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
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
