package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language

/** The gap between the two language choices. */
private val LangGap = 14.dp

@Composable
internal fun LanguageRow(copy: Copy, language: Language, onSetLanguage: (Language) -> Unit) {
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

/** One language choice: label and tick only, the traveling pill carries the surface. */
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
internal fun SoundRow(copy: Copy, muted: Boolean, onToggleMute: () -> Unit) {
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
        // A second, color-only statement of the state: green when sound
        // flows, red when it is switched off.
        Box(Modifier.size(14.dp).background(if (muted) Red else Green, CircleShape))
    }
}
