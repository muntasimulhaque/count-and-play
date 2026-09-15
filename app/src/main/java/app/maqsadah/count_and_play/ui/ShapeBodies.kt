package app.maqsadah.count_and_play.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import app.maqsadah.count_and_play.core.ShapeKind

// ---- Bodies: ten silhouettes, ten distinct classes, no collisions ----------

/** The silhouette of [kind], scaled from its 100-box by [s]. */
internal fun bodyPath(kind: ShapeKind, s: Float): Path = when (kind) {
    ShapeKind.APPLE -> appleBody(s)
    ShapeKind.PEAR -> pearBody(s)
    ShapeKind.STAR -> starBody(s)
    ShapeKind.LEAF -> leafBody(s)
    ShapeKind.BLOCK -> blockBody(s)
    ShapeKind.BEAD -> beadBody(s)
    ShapeKind.MELON -> melonBody(s)
    ShapeKind.CARROT -> carrotBody(s)
    ShapeKind.TULIP -> tulipBody(s)
    ShapeKind.BALL -> ballBody(s)
}

private fun appleBody(s: Float): Path = Path().boxed {
    // A circle with a dimple and a stalk.
    moveTo(50f * s, 24f * s)
    quadraticTo(58f * s, 14f * s, 70f * s, 20f * s)
    quadraticTo(92f * s, 32f * s, 88f * s, 58f * s)
    quadraticTo(84f * s, 88f * s, 50f * s, 90f * s)
    quadraticTo(16f * s, 88f * s, 12f * s, 58f * s)
    quadraticTo(8f * s, 32f * s, 30f * s, 20f * s)
    quadraticTo(42f * s, 14f * s, 50f * s, 24f * s)
    close()
}

private fun pearBody(s: Float): Path = Path().boxed {
    // The only shape whose width changes down its length.
    moveTo(50f * s, 12f * s)
    quadraticTo(70f * s, 16f * s, 66f * s, 40f * s)
    quadraticTo(62f * s, 56f * s, 78f * s, 66f * s)
    quadraticTo(92f * s, 78f * s, 78f * s, 88f * s)
    quadraticTo(64f * s, 96f * s, 50f * s, 94f * s)
    quadraticTo(36f * s, 96f * s, 22f * s, 88f * s)
    quadraticTo(8f * s, 78f * s, 22f * s, 66f * s)
    quadraticTo(38f * s, 56f * s, 34f * s, 40f * s)
    quadraticTo(30f * s, 16f * s, 50f * s, 12f * s)
    close()
}

private fun starBody(s: Float): Path = Path().boxed {
    // Fat-armed, not spiky: spiky stars read badly at small sizes.
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 44f else 25f
        val a = Math.toRadians((-90 + i * 36).toDouble())
        val x = (50f + r * kotlin.math.cos(a).toFloat()) * s
        val y = (52f + r * kotlin.math.sin(a).toFloat()) * s
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun leafBody(s: Float): Path = Path().boxed {
    // A tilted lens with one sharp end.
    moveTo(14f * s, 84f * s)
    quadraticTo(24f * s, 30f * s, 86f * s, 16f * s)
    quadraticTo(72f * s, 74f * s, 14f * s, 84f * s)
    close()
}

private fun blockBody(s: Float): Path = Path().boxed {
    // The only orthogonal shape, instantly separable from every fruit.
    moveTo(16f * s, 36f * s)
    lineTo(46f * s, 12f * s)
    lineTo(88f * s, 12f * s)
    lineTo(88f * s, 62f * s)
    lineTo(58f * s, 88f * s)
    lineTo(16f * s, 88f * s)
    close()
}

private fun beadBody(s: Float): Path {
    // The only shape with a hole in it.
    val outer = Path().apply { addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s)) }
    val hole = Path().apply { addOval(Rect(33f * s, 33f * s, 67f * s, 67f * s)) }
    return Path.combine(PathOperation.Difference, outer, hole)
}

private fun melonBody(s: Float): Path = Path().boxed {
    // A half-disc, flat side up.
    moveTo(8f * s, 30f * s)
    lineTo(92f * s, 30f * s)
    quadraticTo(92f * s, 92f * s, 50f * s, 92f * s)
    quadraticTo(8f * s, 92f * s, 8f * s, 30f * s)
    close()
}

private fun carrotBody(s: Float): Path = Path().boxed {
    // The only downward-pointing wedge.
    moveTo(26f * s, 28f * s)
    lineTo(74f * s, 28f * s)
    quadraticTo(70f * s, 62f * s, 54f * s, 92f * s)
    quadraticTo(50f * s, 97f * s, 46f * s, 92f * s)
    quadraticTo(30f * s, 62f * s, 26f * s, 28f * s)
    close()
}

private fun tulipBody(s: Float): Path = Path().boxed {
    // A three-pointed cup on a stalk. Distinct from the carrot by pointing up.
    moveTo(24f * s, 44f * s)
    lineTo(35f * s, 24f * s)
    lineTo(42f * s, 44f * s)
    lineTo(50f * s, 22f * s)
    lineTo(58f * s, 44f * s)
    lineTo(65f * s, 24f * s)
    lineTo(76f * s, 44f * s)
    quadraticTo(76f * s, 78f * s, 50f * s, 80f * s)
    quadraticTo(24f * s, 78f * s, 24f * s, 44f * s)
    close()
}

private fun ballBody(s: Float): Path = Path().boxed {
    // The only clean circle: nothing else is un-notched, un-holed, un-tipped.
    addOval(Rect(6f * s, 6f * s, 94f * s, 94f * s))
}

/** The lit facet, upper-left, clipped to the body. */
internal fun facetPath(kind: ShapeKind, s: Float): Path = when (kind) {
    ShapeKind.BLOCK ->
        // The block states the light model most plainly: a lit top face.
        Path().boxed {
            moveTo(16f * s, 36f * s)
            lineTo(46f * s, 12f * s)
            lineTo(88f * s, 12f * s)
            lineTo(58f * s, 36f * s)
            close()
        }
    else ->
        Path().boxed {
            moveTo(6f * s, 62f * s)
            quadraticTo(10f * s, 16f * s, 58f * s, 8f * s)
            quadraticTo(30f * s, 26f * s, 30f * s, 62f * s)
            close()
        }
}
