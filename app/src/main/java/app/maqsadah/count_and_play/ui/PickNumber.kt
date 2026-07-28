package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.maqsadah.count_and_play.copy.Copy
import app.maqsadah.count_and_play.core.ShapeKind
import kotlin.math.ceil
import kotlin.math.min

/**
 * "How many shall we use?"
 *
 * The number is the child's. This is the one thing the rebuild took away that
 * he actually noticed was gone — and it costs the mathematics nothing, because
 * what he is choosing from is already bounded by where he is. Choice inside a
 * prepared environment: the ownership is real, the range is not his to break.
 *
 * Every option shows the quantity as objects *and* as a numeral. A 3-year-old
 * choosing "4" from a row of digits is choosing a squiggle; choosing it from
 * four visible apples is choosing four.
 */
@Composable
fun PickNumber(
    prompt: String,
    options: List<Int>,
    shape: ShapeKind,
    copy: Copy,
    onPick: (Int) -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            prompt,
            color = palette.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 52.dp),
        )
        // Three to a row keeps every option a big target even when the range has
        // grown to ten.
        options.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { n ->
                    Option(n, shape, copy, Modifier.weight(1f)) { onPick(n) }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Option(
    n: Int,
    shape: ShapeKind,
    copy: Copy,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
) {
    val palette = LocalPalette.current
    val colors = colorsFor(shape)
    Column(
        modifier
            .fillMaxSize()
            .background(palette.tray, RoundedCornerShape(22.dp))
            .border(4.dp, palette.trayRim, RoundedCornerShape(22.dp))
            .clickable(onClick = onPick)
            .semantics { contentDescription = "$n ${copy.noun(shape, n)}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .drawBehind {
                    val cols = min(n, 5)
                    val rows = ceil(n / cols.toFloat()).toInt()
                    val cell = min(size.width / cols, size.height / rows)
                    val left = (size.width - cell * cols) / 2f
                    val top = (size.height - cell * rows) / 2f
                    for (i in 0 until n) {
                        translate(left + (i % cols) * cell, top + (i / cols) * cell) {
                            drawCountable(shape, cell, colors, palette, detailFor(cell / 2.6f))
                        }
                    }
                },
        )
        Text(
            copy.digits(n),
            color = palette.ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}
