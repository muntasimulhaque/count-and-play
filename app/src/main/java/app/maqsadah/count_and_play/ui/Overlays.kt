package app.maqsadah.count_and_play.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Ladder
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.host.GameUiState

/**
 * The grown-ups panel.
 *
 * Scrollable, with a back handler — the old build put ~500dp of content in a
 * fixed column, so in landscape the Done button sat off-screen with no way
 * back and the panel simply trapped whoever opened it.
 */
@Composable
fun SettingsOverlay(
    state: GameUiState,
    copy: Copy,
    onClose: () -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetSlow: (Boolean) -> Unit,
    onSetLanguage: (Language) -> Unit,
    onAskReset: () -> Unit,
    onConfirmReset: () -> Unit,
) {
    val palette = LocalPalette.current
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(palette.tabletop, RoundedCornerShape(24.dp))
                .border(3.dp, palette.trayRim, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(copy.ui.grownUps, color = palette.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Setting(copy.ui.sound) {
                Toggle(copy.ui.soundOn, state.settings.soundOn) { onSetSound(true) }
                Toggle(copy.ui.soundOff, !state.settings.soundOn) { onSetSound(false) }
            }

            Setting(copy.ui.voice) {
                Toggle(copy.ui.normalVoice, !state.settings.slowRate) { onSetSlow(false) }
                Toggle(copy.ui.slowVoice, state.settings.slowRate) { onSetSlow(true) }
            }

            Setting(copy.ui.language) {
                for (language in Language.entries) {
                    Toggle(language.nativeName, state.settings.language == language) {
                        onSetLanguage(language)
                    }
                }
            }

            if (state.voiceMissing) {
                Text(copy.ui.noVoiceInstalled, color = palette.inkSoft, fontSize = 15.sp)
            }

            Progress(state, copy)

            // Reset is confirm-guarded, because behind a gate is not the same
            // as safe: an adult can still mis-tap it while a child watches.
            if (state.confirmingReset) {
                Text(copy.ui.areYouSure, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Toggle("✔", false, onConfirmReset)
                    Toggle("✖", false, onClose)
                }
            } else {
                Toggle(copy.ui.resetProgress, false, onAskReset)
            }

            Box(Modifier.height(4.dp))
            Toggle(copy.ui.done, true, onClose)
        }
    }
}

/**
 * What the child is working on, in words rather than a score.
 *
 * Deliberately not a number, a percentage or a streak: a progress readout in
 * front of the child turns a co-playing parent into an examiner, and that
 * pressure lands on the child.
 */
@Composable
private fun Progress(state: GameUiState, copy: Copy) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(copy.ui.whatTheyreLearning, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        for (skill in Ladder.unlocked(state.progress)) {
            Text(
                copy.ui.skillLine(nameOf(skill, copy), state.progress.level(skill)),
                color = palette.inkSoft,
                fontSize = 15.sp,
            )
        }
    }
}

private fun nameOf(skill: Skill, copy: Copy): String {
    val english = when (skill) {
        Skill.COUNT -> "Counting"
        Skill.GIVE_N -> "Making a number"
        Skill.COMPARE -> "More and fewer"
        Skill.HIDDEN -> "Hidden adding"
        Skill.JOIN -> "Putting together"
        Skill.SEPARATE -> "Taking away"
    }
    if (copy.language == Language.EN) return english
    return when (skill) {
        Skill.COUNT -> "গোনা"
        Skill.GIVE_N -> "সংখ্যা বানানো"
        Skill.COMPARE -> "বেশি আর কম"
        Skill.HIDDEN -> "লুকানো যোগ"
        Skill.JOIN -> "একসাথে করা"
        Skill.SEPARATE -> "সরিয়ে নেওয়া"
    }
}

@Composable
private fun Setting(label: String, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .background(
                if (selected) palette.tray else palette.trayLiner,
                RoundedCornerShape(14.dp),
            )
            .border(2.dp, palette.trayRim, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .semantics { contentDescription = label },
    ) {
        Text(
            label,
            color = palette.ink,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
