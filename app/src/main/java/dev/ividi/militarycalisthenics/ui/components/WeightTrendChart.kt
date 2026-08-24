package dev.ividi.militarycalisthenics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.AccentYellow
import dev.ividi.militarycalisthenics.ui.theme.TextFaint

/** A small hand-rolled sparkline of bodyweight over time, no charting library. */
@Composable
fun WeightTrendChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val baselineColor = TextFaint
    val lineColors = listOf(AccentOrange, AccentYellow)
    val dotColor = AccentYellow

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (entries.size < 2) return@Canvas

        val minWeight = entries.minOf { it.weightKg }
        val maxWeight = entries.maxOf { it.weightKg }
        val range = (maxWeight - minWeight).coerceAtLeast(1.0)
        val stepX = size.width / (entries.size - 1)
        val padding = 12f

        val points = entries.mapIndexed { index, entry ->
            val x = index * stepX
            val normalized = ((entry.weightKg - minWeight) / range).toFloat()
            val y = size.height - padding - normalized * (size.height - padding * 2)
            Offset(x, y)
        }

        // baseline
        drawLine(
            color = baselineColor.copy(alpha = 0.2f),
            start = Offset(0f, size.height - padding),
            end = Offset(size.width, size.height - padding),
            strokeWidth = 1.dp.toPx()
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                brush = Brush.horizontalGradient(lineColors),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        points.forEach { point ->
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = point)
        }
    }
}
