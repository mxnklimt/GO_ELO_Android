package dev.goelo.android.ui.analysis

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.stats.*
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import java.util.Locale

@Composable fun AnalysisScreen(state: AppState, range: HistoryRange, onRange: (HistoryRange) -> Unit) {
    val selected=if(range.count==null) state.matches else state.matches.takeLast(range.count)
    val summary=summarize(state,range)
    val ranks=byRank(selected)
    val months=byMonth(state.matches)
    val unknownRank=selected.count { it.input==null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        PageHeading("看见你的进步","ANALYSIS  /  棋力分析")
        RangeTabs(range,onRange)
        PanelCard {
            SectionTitle("本区间表现","${summary.games} 盘")
            Box(Modifier.fillMaxWidth().padding(vertical=12.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.size(164.dp)) {
                    val stroke=10.dp.toPx()
                    val arcSize=Size(size.width-stroke,size.height-stroke)
                    drawArc(Gold.copy(alpha=.10f),-90f,360f,false,Offset(stroke/2,stroke/2),arcSize,style=Stroke(stroke))
                    summary.winRate?.let { drawArc(Gold,-90f,(it*360).toFloat(),false,
                        Offset(stroke/2,stroke/2),arcSize,style=Stroke(stroke,cap=StrokeCap.Round)) }
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(summary.winRate?.let { String.format(Locale.US,"%.0f%%",it*100) } ?: "—",
                        style=MaterialTheme.typography.displaySmall,color=Gold)
                    Text("区间胜率",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                AnalysisNumber("胜局","${summary.wins}",Positive,Modifier.weight(1f))
                AnalysisNumber("负局","${summary.games-summary.wins}",Negative,Modifier.weight(1f))
                AnalysisNumber("净变化",deltaText(summary.netDelta),Gold,Modifier.weight(1f))
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text("当前连胜 ${summary.currentStreak}  ·  最长连胜 ${summary.longestStreak}  ·  全部历史",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionTitle("对手段位分布")
        if(ranks.isEmpty()) EmptyPanel("暂无段位分组",
            "此处仅统计临时对手的录入段位。已有棋手对局使用实时等级分，仍计入胜率与月度趋势。",Mark.ANALYSIS)
        else PanelCard {
            ranks.forEach { stat ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("${stat.rank} 段",style=MaterialTheme.typography.titleSmall)
                    Text("${stat.wins} 胜 / ${stat.games} 盘",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress={stat.wins.toFloat()/stat.games},
                    modifier=Modifier.fillMaxWidth().height(6.dp),color=Gold,trackColor=Gold.copy(alpha=.10f),drawStopIndicator={})
            }
            if(unknownRank>0) Text("另有 $unknownRank 盘缺少对手段位，未计入分组。",style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionTitle("月度趋势","全部有日期对局")
        if(months.isEmpty()) EmptyPanel("从今天积累月度轨迹","旧记录没有日期，月度表现会从新对局开始。",Mark.HISTORY)
        else PanelCard {
            months.asReversed().forEach { stat ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stat.month,style=MaterialTheme.typography.titleMedium)
                        Text("${stat.games} 盘 · ${stat.wins} 胜",style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(deltaText(stat.netDelta),color=if(stat.netDelta>=0) Positive else Negative,
                        style=MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
@Composable private fun AnalysisNumber(label: String, value: String, tint: Color, modifier: Modifier) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value,style=MaterialTheme.typography.titleLarge,color=tint)
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
