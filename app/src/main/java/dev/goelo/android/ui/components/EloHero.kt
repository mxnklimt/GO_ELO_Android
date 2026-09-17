package dev.goelo.android.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goelo.android.stats.GoalView
import dev.goelo.android.ui.theme.*

@Composable
fun EloHero(name: String, elo: Double, games: Int, goal: GoalView? = null) {
    Surface(shape=RoundedCornerShape(26.dp),color=Color.Transparent,
        border=BorderStroke(1.dp,Gold.copy(alpha=.24f))) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(
            listOf(Color(0xFF303327),Color(0xFF191E18),Color(0xFF131A16))))) {
            Canvas(Modifier.matchParentSize()) {
                val step=27.dp.toPx()
                val left=size.width*.61f
                for(i in 0..10) {
                    drawLine(Gold.copy(alpha=.055f),Offset(left+i*step,0f),Offset(left+i*step,size.height))
                    drawLine(Gold.copy(alpha=.055f),Offset(left,i*step),Offset(size.width,i*step))
                }
                drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha=.12f),Color.Transparent),
                    center=Offset(size.width,0f),radius=size.width*.8f),radius=size.width*.8f,center=Offset(size.width,0f))
                drawCircle(Gold.copy(alpha=.10f),20.dp.toPx(),Offset(left+2*step,2*step))
                drawCircle(Gold.copy(alpha=.23f),3.dp.toPx(),Offset(left+2*step,2*step))
            }
            Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("当前等级分",style=MaterialTheme.typography.labelLarge,color=Gold,modifier=Modifier.weight(1f))
                    AppMark(Mark.STONE,color=Gold.copy(alpha=.7f))
                }
                Text(eloText(elo),style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.onSurface,
                    maxLines=1, autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(
                        minFontSize=30.sp,
                        maxFontSize=MaterialTheme.typography.displayLarge.fontSize))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=Gold.copy(alpha=.12f),shape=RoundedCornerShape(7.dp)) {
                        Text(goal?.let { "参考 · ${it.rankLabel}" } ?: "个人棋力",Modifier.padding(horizontal=9.dp,vertical=4.dp),
                            style=MaterialTheme.typography.labelSmall,color=Gold)
                    }
                    Text("$name · $games 盘",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier=Modifier.weight(1f))
                }
                if(goal!=null) {
                    Spacer(Modifier.height(9.dp))
                    HorizontalDivider(color=Gold.copy(alpha=.13f))
                    Spacer(Modifier.height(3.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(if(goal.achieved) "目标已达成" else goal.target?.let { "下一站 ${eloText(it)}" } ?: "继续突破自己",
                            style=MaterialTheme.typography.labelMedium,color=Gold,modifier=Modifier.weight(1f))
                        goal.remaining?.let { Text("还差 ${eloText(it)}",style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if(goal.progress!=null) LinearProgressIndicator(
                        progress={goal.progress.toFloat()},modifier=Modifier.fillMaxWidth().height(4.dp),
                        color=Gold,trackColor=Gold.copy(alpha=.12f),drawStopIndicator={})
                }
            }
        }
    }
}
