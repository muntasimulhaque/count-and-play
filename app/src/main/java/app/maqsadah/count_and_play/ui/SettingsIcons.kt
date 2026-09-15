package app.maqsadah.count_and_play.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun CloseButton(modifier: Modifier, description: String, onClose: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .pressable(onClick = onClose)
            .background(Ink.copy(alpha = 0.05f), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 3.5.dp.toPx()
            drawLine(Ink.copy(alpha = 0.75f), Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(Ink.copy(alpha = 0.75f), Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}

/** A speaker with sound waves; when muted, the waves become a single slash. */
@Composable
internal fun SpeakerIcon(muted: Boolean, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val body = Path().apply {
            moveTo(w * 0.05f, h * 0.34f)
            lineTo(w * 0.28f, h * 0.34f)
            lineTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.28f, h * 0.66f)
            lineTo(w * 0.05f, h * 0.66f)
            close()
        }
        drawPath(body, color)
        if (muted) {
            drawLine(Red, Offset(w * 0.60f, h * 0.30f), Offset(w * 0.95f, h * 0.70f), w * 0.10f, StrokeCap.Round)
        } else {
            drawArc(
                color, -50f, 100f, false,
                topLeft = Offset(w * 0.55f, h * 0.30f),
                size = Size(w * 0.34f, h * 0.40f),
                style = Stroke(w * 0.09f, cap = StrokeCap.Round),
            )
            drawArc(
                color, -50f, 100f, false,
                topLeft = Offset(w * 0.62f, h * 0.16f),
                size = Size(w * 0.56f, h * 0.68f),
                style = Stroke(w * 0.09f, cap = StrokeCap.Round),
            )
        }
    }
}
