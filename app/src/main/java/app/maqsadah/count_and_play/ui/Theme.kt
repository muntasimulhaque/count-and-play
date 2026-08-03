package app.maqsadah.count_and_play.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The candy toy-box. One flat, bright playroom: a warm paper ground, a dark
 * toy-chest ink for marks and words, and saturated candy hues. Light is shown
 * as a flat facet on the objects, never as a gradient, blur or shadow.
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

/** The two shared measures: the fat outline everything is drawn with, and the one corner radius. */
val OutlineWidth = 6.dp
val Corner = 28.dp

/**
 * Type stays on the device's own faces on purpose: the app speaks Bengali too,
 * and the system already gives every script its best-shaped font for free.
 */
val ToyFont: FontFamily = FontFamily.Default
val ToyBold: FontWeight = FontWeight.ExtraBold
val ToyBlack: FontWeight = FontWeight.Black

val SizeTitle = 32.sp
val SizePrompt = 26.sp
val SizeLabel = 22.sp
val SizeFlash = 84.sp
val SizeChip = 18.sp
