package app.maqsadah.count_and_play.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.R

/**
 * The candy toy-box. One flat, bright playroom: a warm paper ground, a dark
 * toy-chest ink for marks and words, and saturated candy hues. Objects keep
 * their light as a single hard facet, never a gradient or blur; the chrome
 * (tiles, buttons) gets its weight from a solid darker side edge, the way a
 * wooden key shows its thickness, so everything a finger can press looks
 * pressable.
 */
val Ground = Color(0xFFFFF6E3)
val Ink = Color(0xFF22262E)
val Blue = Color(0xFF1CA9E8)
val Orange = Color(0xFFF7941D)
val Green = Color(0xFF33A852)
val Purple = Color(0xFF8B5CF6)
val Pink = Color(0xFFEC4899)
val Yellow = Color(0xFFFBBF24)
val Liner = Color(0xFFFBFBF9)
val SeatA = Color(0xFFCFE9FB)
val SeatB = Color(0xFFFDE3C4)
val ChipBlue = Color(0xFF27357A)

/** The solid side edge of each pressable key: the rim hue, deepened. */
val BlueEdge = Color(0xFF0B7DB4)
val OrangeEdge = Color(0xFFC86E00)
val GreenEdge = Color(0xFF1F7A3D)
val PinkEdge = Color(0xFFC1266F)
val YellowEdge = Color(0xFFD19B00)

// The fact card's variants of the candy hues: same families, darkened enough
// to clear the 3:1 large-text contrast floor on the white liner. Used only on
// the flash card, where the numerals must survive glare and low vision.
val FlashBlue = Color(0xFF0B72A8)
val FlashOrange = Color(0xFFAD5A00)
val FlashGreen = Color(0xFF1E803E)
val FlashPink = Color(0xFFC2266F)

/** The two shared measures: the fat outline everything is drawn with, and the one corner radius. */
val OutlineWidth = 6.dp
val Corner = 28.dp

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
