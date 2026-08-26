package app.maqsadah.count_and_play.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.R

/**
 * The candy toy-box, hung in a calm gallery. The toys keep every bit of their
 * saturation: objects, chips, badges and confetti carry the hue. The walls go
 * quiet instead: a warm paper ground, white surfaces that float on soft light,
 * and a hairline wherever an edge needs holding. Depth comes from shadow and
 * geometry, never from a gradient or a blur.
 *
 * The material system, one sentence: KEYS float (they are pressed), WELLS hold
 * (they are reached into), CARDS interrupt (the fact card arrives over all).
 */
val Ground = Color(0xFFF6F2EA)
/** Every surface a finger presses: home tiles, sheets. */
val Liner = Color(0xFFFFFFFF)
/**
 * Every surface that HOLDS something: trays, plates, the bowl, the taken box.
 * Pressed paper rather than floating liner: slightly deeper than the ground,
 * so it reads as recessed, not stacked.
 */
val WellFill = Color(0xFFF0EBE1)
val Ink = Color(0xFF22262E)

/** The whisper-thin line that holds a white edge against the paper ground. */
val Hairline = Color(0x1422262E)
/**
 * The key's side, wherever a cap rides: warm sand, darker than the ground,
 * carrying no hue of its own. The games are told apart by their objects,
 * never by trim.
 */
val EdgeNeutral = Color(0xFFDDD3C0)
val Blue = Color(0xFF1CA9E8)
val Orange = Color(0xFFF7941D)
val Green = Color(0xFF33A852)
val Purple = Color(0xFF8B5CF6)
val Pink = Color(0xFFEC4899)
val Yellow = Color(0xFFFBBF24)
val SeatA = Color(0xFFCFE9FB)
val SeatB = Color(0xFFFDE3C4)
val ChipBlue = Color(0xFF27357A)

// The fact card's variants of the candy hues: same families, darkened enough
// to clear the 3:1 large-text contrast floor on the white liner. Used only on
// the flash card, where the numerals must survive glare and low vision.
val FlashBlue = Color(0xFF0B72A8)
val FlashOrange = Color(0xFFAD5A00)
val FlashGreen = Color(0xFF1E803E)
val FlashPink = Color(0xFFC2266F)

/** The one corner radius for big surfaces; smaller elements step down from it. */
val Corner = 28.dp

/**
 * How high the white surfaces hover. Keys ride mid-lift; cards that interrupt
 * (the fact card, the sheet) float higher; wells sit flush with only a contact
 * shadow. The key-cap's own shadow is driven by its press state in [Keys].
 */
val LiftResting = 6.dp
val LiftHeld = 2.dp
val LiftRaised = 12.dp
/** The shadow a well casts where it meets the paper: tight, not hovering. */
val ContactShadow = 1.dp

/**
 * Baloo 2, bundled for the Latin script so the toy-box reads identically on
 * every OEM's default face, and the store art sets the same lettering.
 * Bengali is deliberately not bundled: for that script the system still gives
 * every device its best-shaped face for free, by per-glyph fallback.
 * Licensed under the SIL Open Font License, see docs/OFL-Baloo2.txt.
 */
val ToyFont: FontFamily = FontFamily(
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)
val ToyBold: FontWeight = FontWeight.ExtraBold
val ToyBlack: FontWeight = FontWeight.ExtraBold

val SizeTitle = 32.sp
val SizePrompt = 26.sp
val SizeLabel = 22.sp
val SizeFlash = 84.sp
val SizeChip = 18.sp

/** Corner radius stepped down for small controls (chips of chrome). */
val CornerSmall = 18.dp
