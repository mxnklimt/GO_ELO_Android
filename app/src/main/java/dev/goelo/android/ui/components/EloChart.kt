package dev.goelo.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.goelo.android.stats.SeriesPoint
import dev.goelo.android.ui.theme.*
import kotlin.math.roundToInt

fun nearestPointIndex(x: Float, width: Float, count: Int): Int? {
    if(count==0 || width<=0f) return null
    if(count==1) return 0
    return ((x.coerceIn(0f,width)/width)*(count-1)).roundToInt()
}

@Composable
fun EloChart(points: List<SeriesPoint>, modifier: Modifier = Modifier) {
    var selected by remember(points) { mutableStateOf(points.lastIndex.takeIf { it>=0 }) }
    val min=points.minOfOrNull { it.elo } ?: 0.0
    val max=points.maxOfOrNull { it.elo } ?: 0.0
    val padding=maxOf(5.0,(max-min)*.12)
    val lower=min-padding
    val upper=max+padding
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("ELO",style=MaterialTheme.typography.labelSmall,color=MutedGold)
            Text(eloText(max),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(modifier.semantics { contentDescription="棋力趋势，最低 ${eloText(min)}，最高 ${eloText(max)}" }
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val inset=8.dp.toPx()
                    selected=nearestPointIndex(offset.x-inset,size.width.toFloat()-2*inset,points.size)
                }
            }) {
            val inset=8.dp.toPx()
            val width=(size.width-2*inset).coerceAtLeast(1f)
            fun x(i: Int)=if(points.size==1) size.width/2 else inset+width*i/(points.size-1)
            fun y(elo: Double)=((upper-elo)/(upper-lower)*size.height).toFloat()
            repeat(4) { n -> drawLine(Gold.copy(alpha=.08f),Offset(inset,size.height*n/3),Offset(size.width-inset,size.height*n/3),
                pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),5.dp.toPx()))) }
            if(points.isEmpty()) return@Canvas
            val curve=Path().apply { points.forEachIndexed { i,p -> if(i==0) moveTo(x(i),y(p.elo)) else lineTo(x(i),y(p.elo)) } }
            val area=Path().apply {
                addPath(curve); lineTo(x(points.lastIndex),size.height); lineTo(x(0),size.height); close()
            }
            drawPath(area,Brush.verticalGradient(listOf(Gold.copy(alpha=.22f),Gold.copy(alpha=.01f))))
            drawPath(curve,Gold.copy(alpha=.06f),style=Stroke(8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            drawPath(curve,Gold,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            selected?.let { index ->
                val point=points.getOrNull(index) ?: return@let
                val center=Offset(x(index),y(point.elo))
                drawLine(Gold.copy(alpha=.22f),Offset(center.x,0f),Offset(center.x,size.height),
                    pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),4.dp.toPx())))
                drawCircle(Gold.copy(alpha=.14f),9.dp.toPx(),center)
                drawCircle(Ink,4.5.dp.toPx(),center)
                drawCircle(Gold,3.dp.toPx(),center)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(points.firstOrNull()?.let { if(it.matchId==null) "区间起点" else "第 ${it.order} 盘" } ?: "暂无记录",
                style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(eloText(min),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        selected?.let { index -> points.getOrNull(index)?.let { point ->
            Text((if(point.matchId==null) "起点" else "第 ${point.order} 盘")+"  ·  ${eloText(point.elo)} ELO",
                style=MaterialTheme.typography.labelMedium,color=Gold,modifier=Modifier.padding(top=6.dp))
        } }
    }
}
