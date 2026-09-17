package dev.goelo.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import dev.goelo.android.stats.SeriesPoint
import kotlin.math.roundToInt

fun nearestPointIndex(x: Float, width: Float, count: Int): Int? {
    if (count == 0 || width <= 0f) return null
    if (count == 1) return 0
    return ((x.coerceIn(0f, width) / width) * (count - 1)).roundToInt()
}

@Composable
fun EloChart(points: List<SeriesPoint>, modifier: Modifier = Modifier) {
    var selected by remember(points) { mutableStateOf(points.lastIndex.takeIf { it >= 0 }) }
    Canvas(modifier.pointerInput(points) { detectTapGestures { offset -> selected = nearestPointIndex(offset.x, size.width, points.size) } }) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.elo }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: min
        val lower = if (max == min) min - 10 else min
        val upper = if (max == min) max + 10 else max
        fun x(index: Int) = if (points.size == 1) size.width / 2f else size.width * index / (points.size - 1)
        fun y(value: Double) = size.height - (((value - lower) / (upper - lower)) * size.height).toFloat()
        repeat(4) { grid -> drawLine(Color.White.copy(alpha = .08f), androidx.compose.ui.geometry.Offset(0f, size.height * grid / 3f), androidx.compose.ui.geometry.Offset(size.width, size.height * grid / 3f)) }
        val path = Path().apply { points.forEachIndexed { i, point -> if (i == 0) moveTo(x(i), y(point.elo)) else lineTo(x(i), y(point.elo)) } }
        drawPath(path, Color(0xFFD9BD78), style = Stroke(width = 3f))
        selected?.let { index -> drawCircle(Color(0xFFD9BD78), radius = 7f, center = androidx.compose.ui.geometry.Offset(x(index), y(points[index].elo))) }
    }
}
