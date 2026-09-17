package dev.goelo.android.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.stats.HistoryRange
import dev.goelo.android.stats.byMonth
import dev.goelo.android.stats.byRank
import dev.goelo.android.stats.summarize
import java.util.Locale

@Composable
fun AnalysisScreen(state: AppState, range: HistoryRange, onRange: (HistoryRange) -> Unit) {
    val selected = if (range.count == null) state.matches else state.matches.takeLast(range.count)
    val summary = summarize(state, range)
    val ranks = byRank(selected)
    val months = byMonth(state.matches)
    val unknownRank = selected.count { it.input == null }
    val unknownDate = state.matches.count { it.playedAtEpochMs == null || it.playedZoneId == null }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("分析", style = MaterialTheme.typography.titleLarge)
        RangeSelector(range, onRange)
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("本区间表现", style = MaterialTheme.typography.titleMedium)
            Text("样本 ${summary.games} 盘 · 胜 ${summary.wins} · 胜率 ${summary.winRate?.let { String.format(Locale.US, "%.0f%%", it * 100) } ?: "—"}")
            Text("净变化 ${String.format(Locale.US, "%+.1f", summary.netDelta)} · 当前连胜 ${summary.currentStreak} · 最长连胜 ${summary.longestStreak}")
            Text("参考段位未知/旧历史排除 ${unknownRank} 盘", style = MaterialTheme.typography.bodySmall)
            Text("日期未知/旧历史排除 ${unknownDate} 盘", style = MaterialTheme.typography.bodySmall)
        } }
        Text("按对手参考段位", style = MaterialTheme.typography.titleMedium)
        if (ranks.isEmpty()) Text("暂无可按段位分析的新记录")
        ranks.forEach { stat ->
            ListItem(
                headlineContent = { Text("${stat.rank} 段 · ${stat.games} 盘") },
                supportingContent = { Text("胜 ${stat.wins} · 胜率 ${String.format(Locale.US, "%.0f%%", stat.wins.toDouble() / stat.games * 100)}") },
            )
        }
        Text("月度趋势", style = MaterialTheme.typography.titleMedium)
        Text("全部有日期对局", style = MaterialTheme.typography.bodySmall)
        if (months.isEmpty()) Text("暂无带日期的对局")
        months.asReversed().forEach { stat ->
            ListItem(
                headlineContent = { Text("${stat.month} · ${stat.games} 盘") },
                supportingContent = { Text("胜 ${stat.wins} · 净变化 ${String.format(Locale.US, "%+.1f", stat.netDelta)}") },
            )
        }
    }
}

@Composable
private fun RangeSelector(range: HistoryRange, onRange: (HistoryRange) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HistoryRange.entries.forEach { item ->
            FilterChip(item == range, { onRange(item) }, label = { Text(if (item == HistoryRange.ALL) "全部" else "${item.count}盘") })
        }
    }
}
