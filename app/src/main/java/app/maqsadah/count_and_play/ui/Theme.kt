package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.maqsadah.count_and_play.core.ShapeKind

/**
 * A warm wooden counting tray on a workshop tabletop, lit from the upper left.
 *
 * Flat matte colour, one dark outline per object in its own hue, one hard-edged
 * top facet. No gradients, no glass, no glow — and no faces, so warmth has to
 * come from material, light and weight instead. That is the Lego / Cuisenaire /
 * Montessori tradition, which has kept children happy for a century with no
 * characters in it at all.
 *
 * The single rule that keeps it coherent: the light is always upper-left.
 */
@Immutable
data class Palette(
    val tabletop: Color,
    val tray: Color,
    val trayLiner: Color,
    val trayRim: Color,
    val ink: Color,
    val inkSoft: Color,
    val countedRing: Color,
    val countedChip: Color,
    val onCountedChip: Color,
    val slotOutline: Color,
    val ghostFill: Color,
    val ghostStroke: Color,
    val answerFrame: Color,
    val leaf: Color,
    val leafStroke: Color,
    val dark: Boolean,
)

private val Light = Palette(
    tabletop = Color(0xFFEFE3CC),
    tray = Color(0xFFC9A26B),
    trayLiner = Color(0xFFFFFBF5),
    trayRim = Color(0xFF9A7743),
    ink = Color(0xFF2A2119),
    inkSoft = Color(0xFF6B5B48),
    countedRing = Color(0xFF1F6F4A),
    countedChip = Color(0xFF14532D),
    onCountedChip = Color(0xFFFFFBF5),
    slotOutline = Color(0xFFB49E7C),
    ghostFill = Color(0xFFE0D3BA),
    ghostStroke = Color(0xFF8A7A61),
    answerFrame = Color(0xFF7A5233),
    leaf = Color(0xFF3E8E5A),
    leafStroke = Color(0xFF1E5030),
    dark = false,
)

private val Dark = Palette(
    tabletop = Color(0xFF211C17),
    tray = Color(0xFF4A3B2A),
    trayLiner = Color(0xFF2E2822),
    trayRim = Color(0xFF6B5636),
    ink = Color(0xFFF3E9D8),
    inkSoft = Color(0xFFBFAE95),
    countedRing = Color(0xFF57C08A),
    countedChip = Color(0xFF1F6F4A),
    onCountedChip = Color(0xFFFFFBF5),
    slotOutline = Color(0xFF6E5C43),
    ghostFill = Color(0xFF3A322A),
    ghostStroke = Color(0xFF8A7A61),
    answerFrame = Color(0xFFC0996A),
    leaf = Color(0xFF357C4E),
    leafStroke = Color(0xFF143D24),
    dark = true,
)

/** Fill, outline and top facet for each object. */
@Immutable
data class ShapeColors(val fill: Color, val stroke: Color, val facet: Color)

private val shapeColors = mapOf(
    ShapeKind.APPLE to ShapeColors(Color(0xFFD0402F), Color(0xFF8C2318), Color(0xFFE3675A)),
    ShapeKind.PEAR to ShapeColors(Color(0xFF7E9C22), Color(0xFF4C6110), Color(0xFF9DBB3E)),
    ShapeKind.STAR to ShapeColors(Color(0xFFE8A21B), Color(0xFF8A5A05), Color(0xFFF7BF4E)),
    ShapeKind.LEAF to ShapeColors(Color(0xFF2F8154), Color(0xFF164A2E), Color(0xFF4EA271)),
    ShapeKind.BLOCK to ShapeColors(Color(0xFF2A62B8), Color(0xFF123C7A), Color(0xFF4E85D6)),
    ShapeKind.BEAD to ShapeColors(Color(0xFF7E4399), Color(0xFF4C215F), Color(0xFF9E67B8)),
    ShapeKind.MELON to ShapeColors(Color(0xFFD63A57), Color(0xFF8C1B31), Color(0xFFE86A80)),
    ShapeKind.CARROT to ShapeColors(Color(0xFFD96412), Color(0xFF8A3A05), Color(0xFFEE8B3D)),
    ShapeKind.TULIP to ShapeColors(Color(0xFFC93C75), Color(0xFF821E45), Color(0xFFDE6C99)),
    ShapeKind.BALL to ShapeColors(Color(0xFF0E8894), Color(0xFF04525A), Color(0xFF3AAAB4)),
)

fun colorsFor(shape: ShapeKind): ShapeColors = shapeColors.getValue(shape)

val LocalPalette = staticCompositionLocalOf { Light }

@Composable
fun CountPlayTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (dark) Dark else Light, content = content)
}
