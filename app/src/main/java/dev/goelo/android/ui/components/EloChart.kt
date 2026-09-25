package dev.goelo.android.ui.components

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goelo.android.model.Outcome
import dev.goelo.android.stats.SeriesPoint
import dev.goelo.android.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val LossMarker = Color(0xFF8FAE9F)

fun nearestPointIndex(x: Float, width: Float, count: Int): Int? {
    if (count == 0 || width <= 0f) return null
    if (count == 1) return 0
    return ((x.coerceIn(0f, width) / width) * (count - 1)).roundToInt()
}

@Composable
fun EloChart(points: List<SeriesPoint>, modifier: Modifier = Modifier) {
    var selected by remember(points) { mutableStateOf(points.lastIndex.takeIf { it >= 0 }) }
    val scale = remember(points) { chartScale(points) }
    val markerIndices = remember(points) { visibleMarkerIndices(points.size) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val min = points.minOfOrNull { it.elo } ?: 0.0
    val max = points.maxOfOrNull { it.elo } ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ELO", style = MaterialTheme.typography.labelSmall, color = MutedGold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("● 胜", style = MaterialTheme.typography.labelSmall, color = Gold)
                Text("◆ 负", style = MaterialTheme.typography.labelSmall, color = LossMarker)
            }
        }
        Canvas(modifier.semantics {
            contentDescription = "棋力走势，最低 ${eloText(min)}，最高 ${eloText(max)}。点选查看单盘结果"
        }.pointerInput(points) {
            detectTapGestures { offset ->
                val left = 43.dp.toPx()
                val right = 10.dp.toPx()
                selected = nearestPointIndex(offset.x - left, size.width.toFloat() - left - right, points.size)
            }
        }) {
            val left = 43.dp.toPx()
            val right = size.width - 10.dp.toPx()
            val top = 15.dp.toPx()
            val bottom = size.height - 17.dp.toPx()
            val plotWidth = (right - left).coerceAtLeast(1f)
            val plotHeight = (bottom - top).coerceAtLeast(1f)
            fun x(index: Int) = if (points.size == 1) left + plotWidth / 2 else left + plotWidth * index / (points.size - 1)
            fun y(elo: Double) = top + ((scale.upper - elo) / (scale.upper - scale.lower) * plotHeight).toFloat()
            fun label(value: String, x: Float, y: Float, color: Color, align: AndroidPaint.Align = AndroidPaint.Align.LEFT) {
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    this.color = color.toArgb()
                    textSize = 11.sp.toPx()
                    textAlign = align
                }
                drawContext.canvas.nativeCanvas.drawText(value, x, y, paint)
            }

            scale.ticks.forEach { tick ->
                val tickY = y(tick)
                drawLine(muted.copy(alpha = .15f), Offset(left, tickY), Offset(right, tickY), strokeWidth = 1.dp.toPx())
                label(String.format(Locale.US, "%.0f", tick), left - 6.dp.toPx(), tickY + 4.dp.toPx(), muted, AndroidPaint.Align.RIGHT)
            }
            if (points.isEmpty()) return@Canvas

            val curve = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(x(index), y(point.elo)) else lineTo(x(index), y(point.elo))
                }
            }
            val area = Path().apply {
                addPath(curve)
                lineTo(x(points.lastIndex), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(Gold.copy(alpha = .24f), Gold.copy(alpha = .01f)), startY = top, endY = bottom))
            drawPath(curve, Gold.copy(alpha = .08f), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(curve, Gold, style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            scale.rankEntries.sortedBy { abs(it.elo - points.last().elo) }.take(2).forEach { entry ->
                val guideY = y(entry.elo)
                drawLine(Gold.copy(alpha = .5f), Offset(left, guideY), Offset(right, guideY),
                    strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                label("${entry.rank}段起点", right - 3.dp.toPx(),
                    (guideY - 5.dp.toPx()).coerceIn(top + 10.dp.toPx(), bottom - 4.dp.toPx()),
                    MutedGold, AndroidPaint.Align.RIGHT)
            }

            drawCircle(muted, 2.5.dp.toPx(), Offset(x(0), y(points[0].elo)))
            markerIndices.forEach { index ->
                val point = points[index]
                val center = Offset(x(index), y(point.elo))
                when (point.outcome) {
                    Outcome.WIN -> {
                        drawCircle(Ink, 4.5.dp.toPx(), center)
                        drawCircle(Gold, 3.dp.toPx(), center)
                    }
                    Outcome.LOSS -> {
                        val radius = 4.dp.toPx()
                        val diamond = Path().apply {
                            moveTo(center.x, center.y - radius)
                            lineTo(center.x + radius, center.y)
                            lineTo(center.x, center.y + radius)
                            lineTo(center.x - radius, center.y)
                            close()
                        }
                        drawPath(diamond, LossMarker)
                    }
                    null -> Unit
                }
            }
            selected?.let { index ->
                points.getOrNull(index)?.let { point ->
                    val center = Offset(x(index), y(point.elo))
                    drawLine(Gold.copy(alpha = .23f), Offset(center.x, top), Offset(center.x, bottom),
                        strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                    drawCircle(Gold.copy(alpha = .16f), 9.dp.toPx(), center)
                    drawCircle(Ink, 4.8.dp.toPx(), center)
                    drawCircle(if (point.outcome == Outcome.LOSS) LossMarker else Gold, 3.2.dp.toPx(), center)
                }
            }
            val last = points.last()
            val lastY = y(last.elo)
            val labelY = if (lastY < top + 19.dp.toPx()) lastY + 18.dp.toPx() else lastY - 8.dp.toPx()
            label(eloText(last.elo), right - 2.dp.toPx(), labelY, onSurface, AndroidPaint.Align.RIGHT)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.firstOrNull()?.let { if (it.matchId == null) "区间起点" else "第 ${it.order} 盘" } ?: "暂无记录",
                style = MaterialTheme.typography.labelSmall, color = muted)
            Text(points.lastOrNull()?.let { if (it.matchId == null) "当前" else "当前 · 第 ${it.order} 盘" } ?: "",
                style = MaterialTheme.typography.labelSmall, color = muted)
        }
        selected?.let { index -> points.getOrNull(index)?.let { point ->
            val detail = if (point.matchId == null) "区间起点 · ${eloText(point.elo)} ELO" else {
                val outcome = if (point.outcome == Outcome.WIN) "胜" else "负"
                "第 ${point.order} 盘 · $outcome · ${eloText(point.elo)} ELO · ${deltaText(point.delta ?: 0.0)}"
            }
            Text(detail, style = MaterialTheme.typography.labelMedium, color = Gold, modifier = Modifier.padding(top = 5.dp))
        } }
    }
}
