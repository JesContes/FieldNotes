package com.fieldnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class Glyph { Suitcase, Checklist, Box, Person, Download, Upload, Spark, Trash }

@Composable
fun GlyphIcon(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.8f,
) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val sx = w / 24f
        val sy = h / 24f
        val stroke = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round)
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        when (glyph) {
            Glyph.Suitcase -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(3f, 7f),
                    size = Size(18f * sx, 13f * sy),
                    cornerRadius = CornerRadius(2.5f * sx),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = p(8f, 4f),
                    size = Size(8f * sx, 4f * sy),
                    cornerRadius = CornerRadius(1.5f * sx),
                    style = stroke,
                )
                drawLine(color, p(8f, 10f), p(8f, 17f), strokeWidth * 1.2f * sx, StrokeCap.Round)
                drawLine(color, p(16f, 10f), p(16f, 17f), strokeWidth * 1.2f * sx, StrokeCap.Round)
            }
            Glyph.Checklist -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(4f, 3f),
                    size = Size(16f * sx, 18f * sy),
                    cornerRadius = CornerRadius(2.5f * sx),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = p(8f, 1.5f),
                    size = Size(8f * sx, 4f * sy),
                    cornerRadius = CornerRadius(1.5f * sx),
                    style = stroke,
                )
                drawLine(color, p(8f, 10f), p(16f, 10f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(8f, 14f), p(13f, 14f), strokeWidth * sx, StrokeCap.Round)
            }
            Glyph.Box -> {
                val path = Path().apply {
                    moveTo(3f * sx, 8f * sy)
                    lineTo(12f * sx, 3f * sy)
                    lineTo(21f * sx, 8f * sy)
                    lineTo(12f * sx, 13f * sy)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawLine(color, p(3f, 8f), p(3f, 17f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(21f, 8f), p(21f, 17f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(12f, 13f), p(12f, 22f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(3f, 17f), p(12f, 22f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(21f, 17f), p(12f, 22f), strokeWidth * sx, StrokeCap.Round)
            }
            Glyph.Person -> {
                drawCircle(color, radius = 4f * sx, center = p(12f, 8f), style = stroke)
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = p(4f, 12f),
                    size = Size(16f * sx, 10f * sy),
                    style = stroke,
                )
            }
            Glyph.Download -> {
                drawLine(color, p(12f, 3f), p(12f, 16f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(7f, 11f), p(12f, 16f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(17f, 11f), p(12f, 16f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(5f, 20f), p(19f, 20f), strokeWidth * sx, StrokeCap.Round)
            }
            Glyph.Upload -> {
                drawLine(color, p(12f, 16f), p(12f, 3f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(7f, 8f), p(12f, 3f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(17f, 8f), p(12f, 3f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(5f, 20f), p(19f, 20f), strokeWidth * sx, StrokeCap.Round)
            }
            Glyph.Spark -> {
                drawLine(color, p(12f, 3f), p(12f, 21f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(3f, 12f), p(21f, 12f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(5.5f, 5.5f), p(18.5f, 18.5f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(18.5f, 5.5f), p(5.5f, 18.5f), strokeWidth * sx, StrokeCap.Round)
            }
            Glyph.Trash -> {
                drawLine(color, p(5f, 7f), p(19f, 7f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(9f, 4f), p(15f, 4f), strokeWidth * sx, StrokeCap.Round)
                drawRoundRect(
                    color = color,
                    topLeft = p(7f, 8f),
                    size = Size(10f * sx, 12f * sy),
                    cornerRadius = CornerRadius(2f * sx),
                    style = stroke,
                )
                drawLine(color, p(10f, 11f), p(10f, 17f), strokeWidth * sx, StrokeCap.Round)
                drawLine(color, p(14f, 11f), p(14f, 17f), strokeWidth * sx, StrokeCap.Round)
            }
        }
    }
}
