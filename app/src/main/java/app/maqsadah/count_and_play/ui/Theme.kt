package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.maqsadah.count_and_play.core.ShapeKind
import app.maqsadah.count_and_play.core.Skill

/**
 * A bright playroom. One rule holds the whole thing together:
 *
 * **the objects are the content, so the objects get the contrast.**
 *
 * Trays are white. Every countable therefore sits on the lightest surface on
 * screen, in its own saturated hue, under a dark outline. Colour identity moves
 * to the *ground* and the *rim*, which change per activity — so the six
 * activities stop looking identical without ever competing with the things
 * being counted.
 *
 * The wooden tray this replaced put mid-brown objects on a mid-brown tray on a
 * tan tabletop: about seventy per cent of the screen was one hue family, and
 * the objects had the least contrast of anything on it.
 *
 * Light still falls from the upper left. That part was always right.
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
    /** The seat an object sits on, coloured by the part it came from. */
    val partA: Color,
    val partB: Color,
    val dark: Boolean,
)

private val Light = Palette(
    // tabletop and trayRim are replaced per activity by [forSkill]; these are
    // the resting values, used before an activity has been chosen.
    tabletop = Color(0xFFFDF2D2),
    tray = Color(0xFFFFFFFF),
    trayLiner = Color(0xFFFFFFFF),
    trayRim = Color(0xFFF0B32B),
    ink = Color(0xFF1B2130),
    inkSoft = Color(0xFF5C6678),
    // Indigo, not green: the counted mark appears in every activity, so it must
    // not collide with any activity's own colour.
    countedRing = Color(0xFF3B36B8),
    countedChip = Color(0xFF2A2686),
    onCountedChip = Color(0xFFFFFFFF),
    slotOutline = Color(0xFFC2CAD8),
    ghostFill = Color(0xFFEEF1F6),
    ghostStroke = Color(0xFFA9B2C1),
    // Gold everywhere, whatever the activity: this is the frame the child puts
    // his own answer in, and it should be the same promise every time.
    answerFrame = Color(0xFFF2B705),
    leaf = Color(0xFF35A85C),
    leafStroke = Color(0xFF1B6B37),
    partA = Color(0xFF2E6FD4),
    partB = Color(0xFFF2A03C),
    dark = false,
)

private val Dark = Palette(
    tabletop = Color(0xFF2A2410),
    // Not white at night, but still the lightest thing on screen — the contrast
    // rule survives the theme, the glare does not.
    tray = Color(0xFF2B3340),
    trayLiner = Color(0xFF39424F),
    trayRim = Color(0xFFE0A733),
    ink = Color(0xFFF1F4F9),
    inkSoft = Color(0xFFA8B2C1),
    countedRing = Color(0xFF8C86FF),
    countedChip = Color(0xFF4A45C9),
    onCountedChip = Color(0xFFFFFFFF),
    slotOutline = Color(0xFF5B6675),
    ghostFill = Color(0xFF333B47),
    ghostStroke = Color(0xFF7A8493),
    answerFrame = Color(0xFFE8B93A),
    leaf = Color(0xFF2F9450),
    leafStroke = Color(0xFF15532B),
    partA = Color(0xFF5B92E8),
    partB = Color(0xFFE0A75A),
    dark = true,
)

/** What an activity is dressed in: the rim of its trays, and the ground behind them. */
private data class Skin(val accent: Color, val ground: Color)

private val lightSkins = mapOf(
    Skill.COUNT to Skin(Color(0xFF17A2DC), Color(0xFFDCF1FB)),
    Skill.GIVE_N to Skin(Color(0xFFF5822C), Color(0xFFFFE9D4)),
    Skill.COMPARE to Skin(Color(0xFF8558DE), Color(0xFFEBE3FC)),
    Skill.HIDDEN to Skin(Color(0xFF16B29B), Color(0xFFCFF6F0)),
    Skill.JOIN to Skin(Color(0xFF2FA84F), Color(0xFFDDF5E2)),
    Skill.SEPARATE to Skin(Color(0xFFE8558E), Color(0xFFFCE2ED)),
)

private val darkSkins = mapOf(
    Skill.COUNT to Skin(Color(0xFF3FBCF0), Color(0xFF0C2733)),
    Skill.GIVE_N to Skin(Color(0xFFFF9A4D), Color(0xFF33210F)),
    Skill.COMPARE to Skin(Color(0xFFA37BF0), Color(0xFF231A38)),
    Skill.HIDDEN to Skin(Color(0xFF35CDB6), Color(0xFF0C2E2A)),
    Skill.JOIN to Skin(Color(0xFF4CC46C), Color(0xFF112E19)),
    Skill.SEPARATE to Skin(Color(0xFFF57AA9), Color(0xFF331623)),
)

/** The free tray belongs to no activity, so it gets the resting colour. */
private val lightFree = Skin(Color(0xFFF0B32B), Color(0xFFFDF2D2))
private val darkFree = Skin(Color(0xFFE0A733), Color(0xFF2A2410))

/**
 * Dresses the palette for one activity.
 *
 * Only two fields move. Everything that carries meaning — counted, answer,
 * part A, part B — is constant across the whole app, because a child who has
 * learned that the gold frame is where his answer goes should not have to
 * learn it again on the next screen.
 */
fun Palette.forSkill(skill: Skill?): Palette {
    val skins = if (dark) darkSkins else lightSkins
    val skin = skill?.let(skins::get) ?: if (dark) darkFree else lightFree
    return copy(tabletop = skin.ground, trayRim = skin.accent)
}

/** The accent alone, for tiles on the shelf. */
fun accentFor(skill: Skill?, dark: Boolean): Color {
    val skins = if (dark) darkSkins else lightSkins
    return (skill?.let(skins::get) ?: if (dark) darkFree else lightFree).accent
}

fun groundFor(skill: Skill?, dark: Boolean): Color {
    val skins = if (dark) darkSkins else lightSkins
    return (skill?.let(skins::get) ?: if (dark) darkFree else lightFree).ground
}

/** Fill, outline and top facet for each object. */
@Immutable
data class ShapeColors(val fill: Color, val stroke: Color, val facet: Color)

/**
 * Ten objects that must stay apart from each other on a white tray.
 *
 * Silhouette does the real separating — ten distinct outlines, none of which
 * needs colour to be told apart — so these are free to be simply bright. Every
 * one keeps a much darker outline of its own hue, which is what stops a
 * saturated fill from dissolving into a white background.
 */
private val shapeColors = mapOf(
    ShapeKind.APPLE to ShapeColors(Color(0xFFE33B2C), Color(0xFF8C1D12), Color(0xFFF4695C)),
    ShapeKind.PEAR to ShapeColors(Color(0xFF8FB023), Color(0xFF4C6110), Color(0xFFB2CE46)),
    ShapeKind.STAR to ShapeColors(Color(0xFFF5AC17), Color(0xFF8A5A05), Color(0xFFFFC94F)),
    ShapeKind.LEAF to ShapeColors(Color(0xFF2F9E60), Color(0xFF135233), Color(0xFF56BC85)),
    ShapeKind.BLOCK to ShapeColors(Color(0xFF2D6FDB), Color(0xFF123C7A), Color(0xFF5A95EE)),
    ShapeKind.BEAD to ShapeColors(Color(0xFF9046B5), Color(0xFF4C215F), Color(0xFFB073CE)),
    ShapeKind.MELON to ShapeColors(Color(0xFFEE3D5F), Color(0xFF8C1B31), Color(0xFFFA7391)),
    ShapeKind.CARROT to ShapeColors(Color(0xFFF06A0E), Color(0xFF8A3A05), Color(0xFFFF9440)),
    ShapeKind.TULIP to ShapeColors(Color(0xFFE04384), Color(0xFF821E45), Color(0xFFF278AA)),
    ShapeKind.BALL to ShapeColors(Color(0xFF0EA0AE), Color(0xFF04525A), Color(0xFF44C0CC)),
)

fun colorsFor(shape: ShapeKind): ShapeColors = shapeColors.getValue(shape)

val LocalPalette = staticCompositionLocalOf { Light }

@Composable
fun CountPlayTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (dark) Dark else Light, content = content)
}
