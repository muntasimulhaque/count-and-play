package app.maqsadah.count_and_play.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy

/** Prompt on top, play in the middle, and always a small house top-left. */
@Composable
internal fun ActivityFrame(prompt: String, copy: Copy, onHome: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        FrameHeader(prompt, copy, onHome)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}

/** The house and the ask, side by side at the top of every round. */
@Composable
private fun FrameHeader(prompt: String, copy: Copy, onHome: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeButton(copy.homeLabel(), onHome)
        PromptText(prompt, reducedMotion)
        // Mirrors the home button so the prompt stays optically centered and
        // can never slide underneath it, whatever the screen width or font.
        Spacer(Modifier.width(52.dp))
    }
}

/**
 * The ask itself. It cross-fades when the game's phase rewrites it, so the
 * words trade places gently instead of hard-swapping mid-play.
 */
@Composable
private fun RowScope.PromptText(prompt: String, reducedMotion: Boolean) {
    AnimatedContent(
        targetState = prompt,
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (
                    fadeIn(tween(durationMillis = 180)) +
                        slideInVertically(tween(durationMillis = 180)) { it / 3 }
                    ) togetherWith fadeOut(tween(durationMillis = 120))
            }
        },
        label = "prompt",
        modifier = Modifier.weight(1f),
    ) { text ->
        Text(
            text,
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Ink,
            fontSize = SizePrompt,
            // Tight leading and a two-line ceiling: a long ask at the
            // capped font scale must not eat the play area below it.
            lineHeight = SizePrompt * 1.15f,
            maxLines = 2,
            fontWeight = ToyBold,
            fontFamily = ToyFont,
        )
    }
}

@Composable
private fun HomeButton(description: String, onHome: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .pressable(onClick = onHome)
            .background(Liner, CircleShape)
            .border(BorderStroke(1.dp, Hairline), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        HouseIcon(28.dp, Ink.copy(alpha = 0.85f))
    }
}

/** A plain house: roof, wall, door. Vector, static, no emoji. */
@Composable
private fun HouseIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val roof = Path().apply {
            moveTo(w * 0.04f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.96f, h * 0.48f)
            close()
        }
        drawPath(roof, color)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.16f, h * 0.44f),
            size = Size(w * 0.68f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.08f),
        )
        drawRoundRect(
            Liner,
            topLeft = Offset(w * 0.40f, h * 0.62f),
            size = Size(w * 0.20f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
    }
}
