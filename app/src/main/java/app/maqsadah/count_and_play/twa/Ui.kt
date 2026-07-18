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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/* ---------- CSS clamp() equivalents (vmin in dp, treated like the web's px) ---------- */

fun clampSp(minV: Float, vminPct: Float, maxV: Float, vmin: Float): TextUnit =
    max(minV, min(maxV, vminPct / 100f * vmin)).sp

fun clampDp(minV: Float, vminPct: Float, maxV: Float, vmin: Float): Dp =
    max(minV, min(maxV, vminPct / 100f * vmin)).dp

fun itemFont(size: SizeClass, vmin: Float): TextUnit = when (size) {
    SizeClass.BIG -> clampSp(36f, 9f, 80f, vmin)
    SizeClass.MID -> clampSp(26f, 6.5f, 56f, vmin)
    SizeClass.SMALL -> clampSp(20f, 4.8f, 40f, vmin)
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

/* ---------- a flowing group of items, centered ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemsFlow(
    items: List<Item>,
    fontSize: TextUnit,
    gap: Dp,
    onTap: ((Item) -> Unit)? = null
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)
    ) {
        items.forEach { itm ->
            key(itm.id) {
                ItemView(itm, fontSize, onTap)
            }
        }
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
