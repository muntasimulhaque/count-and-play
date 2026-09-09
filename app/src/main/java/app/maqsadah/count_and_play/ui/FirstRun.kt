package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language

/** The one-time door: nothing is playable until a language has been chosen. */
@Composable
fun FirstRunPicker(copy: Copy, onSetLanguage: (Language) -> Unit) {
    // Opaque and tap-swallowing: no touch reaches the shelf beneath it.
    Box(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        FadeIn {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .shadow(elevation = LiftRaised, shape = RoundedCornerShape(Corner), clip = false)
                    .background(Liner, RoundedCornerShape(Corner))
                    .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(Corner))
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
}

@Composable
private fun LangButton(name: String, active: Boolean, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            // A floor, not a ceiling: the box grows with the text under the
            // capped font scale, so no label ever clips at any setting.
            .heightIn(min = if (big) 72.dp else 56.dp)
            .pressable(onClick = onClick)
            .background(
                if (active) Blue.copy(alpha = 0.12f) else Liner,
                RoundedCornerShape(CornerSmall),
            )
            .border(
                BorderStroke(if (active) 2.dp else 1.dp, if (active) Blue else Hairline),
                RoundedCornerShape(CornerSmall),
            )
            .semantics(mergeDescendants = true) {
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

/** A small check that draws itself on, arm first, then the long tail. */
@Composable
internal fun TickMark(color: Color, size: Dp) {
    val reducedMotion = rememberReducedMotion()
    val draw = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && draw.value < 1f) draw.animateTo(1f, tween(durationMillis = 240, delayMillis = 90))
    }
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.16f
        // Two strokes in sequence, so the tick reads as drawn, not stamped.
        val arm = (draw.value * 2f).coerceAtMost(1f)
        if (arm > 0f) {
            drawLine(
                color,
                Offset(w * 0.12f, h * 0.55f),
                Offset(w * 0.12f, h * 0.55f) + Offset(w * 0.26f, h * 0.30f) * arm,
                stroke, StrokeCap.Round,
            )
        }
        val tail = ((draw.value - 0.5f) * 2f).coerceIn(0f, 1f)
        if (tail > 0f) {
            drawLine(
                color,
                Offset(w * 0.38f, h * 0.85f),
                Offset(w * 0.38f, h * 0.85f) + Offset(w * 0.50f, -h * 0.70f) * tail,
                stroke, StrokeCap.Round,
            )
        }
    }
}
