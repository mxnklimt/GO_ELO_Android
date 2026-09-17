package dev.goelo.android.ui.growth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.stats.HistoryRange
import dev.goelo.android.stats.series
import dev.goelo.android.stats.summarize
import dev.goelo.android.ui.components.EloChart
import dev.goelo.android.ui.components.EloHero
import java.util.Locale

@Composable
fun GrowthScreen(state: AppState, range: HistoryRange, onRange: (HistoryRange) -> Unit, onRecord: () -> Unit,
    onBackup: () -> Unit, undoAvailable: Boolean = false, onUndo: () -> Unit = {}) {
    val profile = requireNotNull(state.profile)
    val current = state.matches.maxByOrNull { it.order }?.eloAfter ?: profile.initialElo
    val summary = summarize(state, range)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("成长", style = MaterialTheme.typography.titleLarge)
        EloHero(profile.name, current, state.matches.size)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            HistoryRange.entries.forEach { item -> FilterChip(selected = item == range, onClick = { onRange(item) }, label = { Text(if (item == HistoryRange.ALL) "全部" else "${item.count}盘") }) }
        }
        Card { Column(Modifier.padding(16.dp)) { EloChart(series(state, range), Modifier.fillMaxWidth().height(180.dp)); Text("区间起点 · ${String.format(Locale.US, "%.1f", series(state, range).firstOrNull()?.elo ?: current)}", style = MaterialTheme.typography.labelSmall) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("胜率", summary.winRate?.let { String.format(Locale.US, "%.0f%%", it * 100) } ?: "—", Modifier.weight(1f))
            StatCard("净变化", String.format(Locale.US, "%+.1f", summary.netDelta), Modifier.weight(1f))
            StatCard("最高分", String.format(Locale.US, "%.1f", summary.peak), Modifier.weight(1f))
        }
        if (state.matches.isNotEmpty()) Text("最近对局", style = MaterialTheme.typography.titleMedium)
        state.matches.takeLast(5).asReversed().forEach { match -> ListItem(headlineContent = { Text(if (match.outcome.name == "WIN") "胜" else "负") }, supportingContent = { Text("${String.format(Locale.US, "%+.1f", match.delta)} · ${String.format(Locale.US, "%.1f", match.eloAfter)}") }) }
        Button(onClick = onRecord, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("记一盘") }
        if (undoAvailable) TextButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) { Text("撤销") }
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier) = Card(modifier) { Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } }
