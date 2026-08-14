package app.maqsadah.count_and_play.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A burst of confetti when something goes right.
 *
 * Squares, discs and bars: nothing with a top and a bottom, nothing that
 * could resolve into a creature at a glance. Physical delight is allowed; a
 * dancing mascot is not, and paper thrown in the air is about as physical as
 * a screen gets.
 *
 * It never blocks. A celebration the game has to stop and wait for is not a
 * celebration, it is another pause, so the voice names the mathematics at
 * the same moment the paper is still coming down.
 */
private class Fleck(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val size: Float,
    val kind: Int,
    val color: Color,
)

private val confettiColors = listOf(Blue, Orange, Green, Purple, Pink, Yellow)

/**
 * Fires one burst every time [key] changes to a new non-zero value.
 * The fall runs ~1.5 s and draws nothing once it has landed.
 */
@Composable
fun Sparkle(key: Int, modifier: Modifier = Modifier) {
    // Fixed seed: every burst looks the same, so success has one face.
    val flecks = remember {
        val random = Random(4)
        List(24) {
            Fleck(
                // Thrown upward and outward, never straight down.
                angle = -155f + random.nextFloat() * 130f,
                speed = 0.34f + random.nextFloat() * 0.42f,
                spin = -540f + random.nextFloat() * 1080f,
                size = 0.020f + random.nextFloat() * 0.022f,
                kind = random.nextInt(3),
                color = confettiColors[random.nextInt(confettiColors.size)],
            )
        }
    }
    val flight = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key == 0) return@LaunchedEffect
        flight.snapTo(0f)
        flight.animateTo(1f, tween(durationMillis = 1500, easing = LinearEasing))
    }
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val t = flight.value
                if (t <= 0f || t >= 1f) return@drawBehind
                val origin = Offset(size.width / 2f, size.height * 0.42f)
                val reach = size.minDimension
                for (fleck in flecks) {
                    val radians = Math.toRadians(fleck.angle.toDouble())
                    val x = origin.x + cos(radians).toFloat() * fleck.speed * reach * t
                    // Thrown up, then gravity takes it: t - t^2 rises and falls.
                    val y = origin.y + sin(radians).toFloat() * fleck.speed * reach * t +
                        reach * 1.15f * t * t
                    val side = fleck.size * reach
                    val fade = if (t < 0.7f) 1f else 1f - (t - 0.7f) / 0.3f
                    rotate(fleck.spin * t, pivot = Offset(x, y)) {
                        drawFleck(fleck.kind, Offset(x, y), side, fleck.color.copy(alpha = fade))
                    }
                }
            },
    )
}

private fun DrawScope.drawFleck(kind: Int, at: Offset, side: Float, color: Color) {
    when (kind) {
        0 -> drawCircle(color, side / 2f, at)
        1 -> drawRect(color, Offset(at.x - side / 2f, at.y - side / 2f), Size(side, side))
        // A thin bar: the shape a real piece of confetti actually is.
        else -> drawRect(color, Offset(at.x - side / 2f, at.y - side / 6f), Size(side, side / 3f))
    }
}
