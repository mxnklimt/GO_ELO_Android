package dev.goelo.android.ui.growth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.stats.*
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import java.util.Locale

@Composable
fun GrowthScreen(state: AppState, range: HistoryRange, onRange: (HistoryRange) -> Unit, onRecord: () -> Unit,
    onBackup: () -> Unit, undoAvailable: Boolean = false, onUndo: () -> Unit = {}) {
    val profile=requireNotNull(state.profile)
    val current=state.matches.maxByOrNull { it.order }?.eloAfter ?: profile.initialElo
    val summary=summarize(state,range)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        PageHeading("每一盘，向上。","GO ELO  /  个人棋力成长") {
            FilledTonalIconButton(onClick=onBackup,modifier=Modifier.semantics { contentDescription="备份" }) {
                AppMark(Mark.BACKUP)
            }
        }
        EloHero(profile.name,current,state.matches.size,goalView(profile,current))
        PanelCard {
            SectionTitle("棋力走势",if(range==HistoryRange.ALL) "全部对局" else "最近 ${range.count} 盘")
            RangeTabs(range,onRange)
            EloChart(series(state,range),Modifier.fillMaxWidth().height(170.dp))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Metric("区间胜率",summary.winRate?.let { String.format(Locale.US,"%.0f%%",it*100) } ?: "—",Modifier.weight(1f))
            Metric("区间净变化",deltaText(summary.netDelta),Modifier.weight(1f))
            Metric("当前连胜","${summary.currentStreak}",Modifier.weight(1f))
        }
        if(state.matches.isEmpty()) EmptyPanel("从第一盘开始","记录一场对局，让每一次进步都有迹可循。")
        else {
            SectionTitle("最近对局","共 ${state.matches.size} 盘")
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                state.matches.takeLast(5).asReversed().forEach { ResultRow(it) }
            }
        }
        Text("历史最高  ${eloText(summary.peak)} ELO",style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier,color=Panel,shape=MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal=10.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,style=MaterialTheme.typography.titleLarge,color=Gold)
        }
    }
}
