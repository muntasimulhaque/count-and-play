package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Room for two digits ("10" / "১০") without going oval: the chip grows with
// the object it tags, from a floor that stays legible on small trays.
internal fun chipDiameter(objectSize: Dp): Dp = maxOf(30.dp, objectSize * 0.36f)

/**
 * The numbered chip in the child's own tap order. A fixed square, so it stays
 * a true circle even for two-digit chips like 10: aspectRatio under loose
 * constraints would balloon it to fill the tray.
 */
@Composable
internal fun CountChip(text: String, diameter: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(diameter)
            .clearAndSetSemantics { }
            .background(ChipBlue, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = (diameter.value * 0.58f).sp,
            lineHeight = (diameter.value * 0.58f).sp,
            fontWeight = ToyBlack,
            fontFamily = ToyFont,
        )
    }
}

/**
 * The dashed outline left where a taken-away object used to sit. It occupies
 * the same node an [ObjectView] does, so rows keep one rhythm whether a cell
 * holds an object or the ghost of one. The dash itself stays at the body
 * size, deliberately quiet so it is not read as an object.
 */
@Composable
fun GhostSlot(sizeDp: Dp) {
    Box(Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            drawEmptySlot(size.minDimension)
        }
    }
}
