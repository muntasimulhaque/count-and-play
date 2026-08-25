package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.host.Flash

/**
 * The big numeral moment after a round completes: the arithmetic itself is
 * the praise, so the card shows nothing else. The scrim fades up while the
 * card pops from 0.6 to full, so arrival has one motion, not two. The card
 * is a polite live region, so a screen reader announces the fact too.
 */
@Composable
fun FlashOverlay(flash: Flash, copy: Copy) {
    val reducedMotion = rememberReducedMotion()
    val scrim = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.6f) }
    LaunchedEffect(flash, reducedMotion) {
        if (reducedMotion) {
            scrim.snapTo(1f)
        } else {
            scrim.snapTo(0f)
            scrim.animateTo(1f, tween(durationMillis = 220))
        }
    }
    LaunchedEffect(flash, reducedMotion) {
        if (!reducedMotion && scale.value < 1f) scale.animateTo(1f, PopSpring)
    }
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.45f * scrim.value)),
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

/** The fact itself, on a floating white card: no ribbon, no border colour. */
@Composable
private fun FactCard(flash: Flash, copy: Copy, modifier: Modifier = Modifier) {
    Column(
        modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .shadow(elevation = LiftRaised, shape = RoundedCornerShape(Corner), clip = false)
            .background(Liner, RoundedCornerShape(Corner))
            .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(Corner))
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
