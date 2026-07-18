package app.maqsadah.count_and_play.twa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/* ---------- CSS clamp() equivalents (vmin in dp, treated like the web's px) ---------- */

fun clampSp(minV: Float, vminPct: Float, maxV: Float, vmin: Float): TextUnit =
    max(minV, min(maxV, vminPct / 100f * vmin)).sp

fun clampDp(minV: Float, vminPct: Float, maxV: Float, vmin: Float): Dp =
    max(minV, min(maxV, vminPct / 100f * vmin)).dp

/* ---------- item sizing ----------
 * Counting objects render at ONE fixed size across the whole app — a star in a
 * "1" box is exactly as big as each star in a "14" box, and objects never change
 * size from one problem to the next. The size is computed once (in Stage) as the
 * largest emoji that fits the worst case — [G.MAX_N] objects in a main plate — and
 * handed to every zone; each zone only shrinks BELOW it as a non-clipping safety
 * net if its own box genuinely can't hold that many at that size (e.g. a crowded
 * take-away area). Size depends only on the box + count, never the emoji, so
 * mango, car or ball all render identically.
 *
 * The layout is a DETERMINISTIC grid, not a free-flowing FlowRow: we compute the
 * column count and fixed cell size ourselves and place items in exactly that many
 * columns. This guarantees what we measured is what we render, so rows can never
 * overflow the (clipped) box and silently drop objects — the "8+8 shows only 6"
 * bug came from FlowRow wrapping into more rows than the fit estimate assumed.
 */
// Cell footprint per glyph, as a multiple of font size. Kept safely LARGER than a
// real emoji's advance width / line height so the glyph always sits inside its
// cell and never spills past the box edge to be clipped away.
private const val CELL_W_RATIO = 1.30f
private const val CELL_H_RATIO = 1.55f
private const val ITEM_MIN_SP = 12f
private const val ITEM_MAX_SP = 92f

/** Columns that fit across [availWidthPx] for a glyph of [fontSp] with [gapPx] spacing. */
fun gridCols(fontSp: TextUnit, availWidthPx: Float, gapPx: Float, density: Density): Int {
    val fontPx = with(density) { fontSp.toPx() }
    val cellW = fontPx * CELL_W_RATIO
    return max(1, floor((availWidthPx + gapPx) / (cellW + gapPx)).toInt())
}

/**
 * Largest emoji font (sp) so that [count] square-ish items fit inside a box of
 * [availWidthPx] × [availHeightPx] with [gapPx] spacing, laid out like FlowRow.
 * Returns [ITEM_MIN_SP] as a floor if even that will not fit.
 */
fun fitItemFontSp(
    availWidthPx: Float,
    availHeightPx: Float,
    count: Int,
    gapPx: Float,
    density: Density
): TextUnit {
    if (count <= 0 || availWidthPx <= 0f || availHeightPx <= 0f) return ITEM_MAX_SP.sp
    var f = ITEM_MAX_SP
    while (f > ITEM_MIN_SP) {
        val fontPx = with(density) { f.sp.toPx() }
        val cellW = fontPx * CELL_W_RATIO
        val cellH = fontPx * CELL_H_RATIO
        val perRow = max(1, floor((availWidthPx + gapPx) / (cellW + gapPx)).toInt())
        val rows = ceil(count.toFloat() / perRow).toInt()
        val neededH = rows * cellH + (rows - 1) * gapPx
        if (cellW <= availWidthPx && neededH <= availHeightPx) break
        f -= 1f
    }
    return f.sp
}

/* ---------- chunky 3D-shadow button (the app's signature look) ---------- */

@Composable
fun ChunkyButton(
    text: String,
    bg: Color,
    shadow: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(26.dp),
    enabled: Boolean = true,
    fillSize: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    onClick: () -> Unit
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.25f)
            .clip(shape)
            .background(shadow)
            .clickable(enabled = enabled) { onClick() }
    ) {
        val inner = if (fillSize) {
            Modifier.padding(bottom = 5.dp).fillMaxSize()
        } else {
            Modifier.padding(bottom = 5.dp)
        }
        Box(
            inner.clip(shape).background(bg).then(
                if (fillSize) Modifier else Modifier.padding(contentPadding)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------- a zone (plate / take-away area) with dashed border ---------- */

@Composable
fun ZoneBox(
    modifier: Modifier,
    borderColor: Color,
    bg: Color = Palette.ZoneBg,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                    ),
                    cornerRadius = CornerRadius(22.dp.toPx())
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/* ---------- one emoji item with drop-in / pulse / ghost / count bubble ---------- */

@Composable
fun ItemView(item: Item, fontSize: TextUnit, onTap: ((Item) -> Unit)? = null) {
    val drop = remember { Animatable(if (item.dropped) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!item.dropped) {
            item.dropped = true
            drop.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(item.pulseTick) {
        if (item.pulseTick > 0) {
            pulse.animateTo(1.4f, tween(250))
            pulse.animateTo(1f, tween(250))
        }
    }
    LaunchedEffect(item.groupPulseTick) {
        if (item.groupPulseTick > 0) {
            pulse.animateTo(1.4f, tween(400))
            pulse.animateTo(1f, tween(400))
        }
    }
    val bubbleOffset = with(LocalDensity.current) { fontSize.toDp() * 0.55f }

    Box(contentAlignment = Alignment.Center) {
        Text(
            item.emoji,
            fontSize = fontSize,
            modifier = Modifier
                .graphicsLayer {
                    val t = drop.value
                    translationY = (1f - t) * (-50).dp.toPx()
                    val s = pulse.value * (if (item.ghost) 0.85f else 1f) * (0.3f + 0.7f * t)
                    scaleX = s
                    scaleY = s
                    alpha = (if (item.ghost) 0.35f else 1f) * min(1f, t / 0.7f)
                }
                .then(
                    if (onTap != null) {
                        Modifier.clickable(enabled = item.tappable) { onTap(item) }
                    } else Modifier
                )
        )
        item.bubble?.let { b ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -bubbleOffset)
                    .background(Palette.Yellow, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    "$b",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize * 0.5f
                )
            }
        }
    }
}

/* ---------- a deterministic, centered grid of fixed-size cells ----------
 * Places [items] in exactly [cols] columns of [cellW] × [cellH] cells. Because we
 * fix the column count and cell size, the rendered row count equals ceil(n/cols)
 * exactly — nothing can wrap into an extra, clipped row. Rows fill left-to-right
 * in list order, so appending an item never reshuffles the ones already placed
 * (their drop-in animation isn't replayed).
 */
@Composable
fun <T> CenteredGrid(
    items: List<T>,
    cols: Int,
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    keyOf: (Int, T) -> Any,
    cell: @Composable (T) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.chunked(max(1, cols)).forEachIndexed { r, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                rowItems.forEachIndexed { ci, itm ->
                    key(keyOf(r * cols + ci, itm)) {
                        Box(Modifier.size(cellW, cellH), contentAlignment = Alignment.Center) {
                            cell(itm)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- a group of items sized to the app-wide fixed object size ----------
 * [capacity] is how many items this box will ultimately hold this round; we plan
 * the grid for that (not the current count) so objects don't resize as they drop
 * in one by one. [fixedFontSp] is the single app-wide object size computed in
 * Stage. This box renders at that size, only shrinking below it if [capacity]
 * genuinely will not fit here at that size (a non-clipping safety net).
 */
@Composable
fun AutoItemsFlow(
    items: List<Item>,
    capacity: Int,
    gap: Dp,
    fixedFontSp: TextUnit,
    modifier: Modifier = Modifier,
    onTap: ((Item) -> Unit)? = null
) {
    BoxWithConstraints(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val n = max(capacity, items.size)
        // Fixed size everywhere; only shrink if this exact box can't fit n at it.
        val ownFit = fitItemFontSp(wPx, hPx, n, gapPx, density)
        val fs = if (fixedFontSp.value <= ownFit.value) fixedFontSp else ownFit
        val cols = min(n, gridCols(fs, wPx, gapPx, density))
        val cellW = with(density) { (fs.toPx() * CELL_W_RATIO).toDp() }
        val cellH = with(density) { (fs.toPx() * CELL_H_RATIO).toDp() }
        CenteredGrid(
            items = items,
            cols = cols,
            cellW = cellW,
            cellH = cellH,
            gap = gap,
            keyOf = { _, itm -> itm.id }
        ) { itm -> ItemView(itm, fs, onTap) }
    }
}

/* ---------- confetti ---------- */

private class ConfettiPiece(
    val id: Int,
    val emoji: String,
    val xFrac: Float,
    val durMs: Int,
    val delayMs: Long
)

@Composable
fun ConfettiOverlay(tick: Int) {
    val pieces = remember { mutableStateListOf<ConfettiPiece>() }
    val counter = remember { intArrayOf(0) }
    LaunchedEffect(tick) {
        if (tick > 0) {
            val fx = listOf("🎉", "⭐", "🌟", "✨", "🎈")
            repeat(18) {
                counter[0]++
                pieces.add(
                    ConfettiPiece(
                        id = counter[0],
                        emoji = fx[G.rand(fx.size)],
                        xFrac = G.rand(100) / 100f,
                        durMs = (1400 + G.randF() * 1400).toInt(),
                        delayMs = (G.randF() * 400).toLong()
                    )
                )
            }
        }
    }
    if (pieces.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        pieces.toList().forEach { p ->
            key(p.id) {
                val prog = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(p.delayMs)
                    prog.animateTo(1f, tween(p.durMs, easing = LinearEasing))
                    pieces.remove(p)
                }
                Text(
                    p.emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = p.xFrac * wPx
                        translationY = -80f + prog.value * (hPx + 160f)
                        rotationZ = prog.value * 720f
                    }
                )
            }
        }
    }
}
