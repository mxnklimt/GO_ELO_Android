package dev.goelo.android.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import dev.goelo.android.stats.HistoryFilter
import dev.goelo.android.stats.filterMatches
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    matches: List<Match>,
    revision: Long,
    busy: Boolean,
    error: String?,
    onEdit: (String, RecordInput, Outcome, Long) -> Unit,
    onDelete: (String, Long) -> Unit,
) {
    var rank by remember { mutableStateOf<Int?>(null) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    var undated by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Match?>(null) }
    var selectedRevision by remember { mutableLongStateOf(0L) }
    var deleting by remember { mutableStateOf<Match?>(null) }
    var editRank by remember { mutableIntStateOf(7) }
    var editRecord by remember { mutableStateOf("") }
    var editOutcome by remember { mutableStateOf(Outcome.WIN) }
    val filter = HistoryFilter(rank, outcome, fromDate.ifBlank { null }, toDate.ifBlank { null }, undated)
    val result = remember(matches, filter) { runCatching { filterMatches(matches, filter) } }
    val visible = result.getOrDefault(emptyList())
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("历史", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            FilterChip(rank == null, { rank = null }, label = { Text("全部段位") })
            (1..9).forEach { value -> FilterChip(rank == value, { rank = value }, label = { Text("$value 段") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(outcome == null, { outcome = null }, label = { Text("全部结果") })
            FilterChip(outcome == Outcome.WIN, { outcome = Outcome.WIN }, label = { Text("胜") })
            FilterChip(outcome == Outcome.LOSS, { outcome = Outcome.LOSS }, label = { Text("负") })
            FilterChip(undated, { undated = !undated }, label = { Text("仅旧历史") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(fromDate, { fromDate = it }, label = { Text("起始日期") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(toDate, { toDate = it }, label = { Text("结束日期") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        result.exceptionOrNull()?.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("没有符合条件的对局") }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(visible, key = { it.id }) { match ->
                MatchRow(match, onClick = {
                    if (match.kind == MatchKind.NATIVE) {
                        selected = match
                        selectedRevision = revision
                        editRank = requireNotNull(match.input).rank
                        editRecord = "${match.input!!.wins}-${match.input!!.losses}"
                        editOutcome = match.outcome
                    } else selected = match
                })
            }
        }
    }
    val open = selected
    if (open?.kind == MatchKind.NATIVE) EditMatchSheet(
        match = open, rank = editRank, record = editRecord, outcome = editOutcome,
        affectedCount = matches.count { it.order > open.order }, busy = busy, error = error,
        onRank = { editRank = it }, onRecord = { editRecord = it }, onOutcome = { editOutcome = it },
        onSave = {
            val input = dev.goelo.android.rating.parseRecord(editRank, editRecord).getOrNull() ?: return@EditMatchSheet
            onEdit(open.id, input, editOutcome, selectedRevision)
            selected = null
        }, onDelete = { selected = null; deleting = open }, onDismiss = { if (!busy) selected = null },
    )
    if (open?.kind == MatchKind.LEGACY) AlertDialog(
        onDismissRequest = { selected = null }, title = { Text("旧历史记录") },
        text = { Text("旧版导入记录按原始变动保存，不能编辑或删除。如需调整，请在“我的”页面重新导入旧文件。") },
        confirmButton = { TextButton(onClick = { selected = null }) { Text("知道了") } },
    )
    deleting?.let { match -> AlertDialog(
        onDismissRequest = { if (!busy) deleting = null }, title = { Text("删除对局？") },
        text = { Text("将删除：${deleteDetail(match)}\n\n并重算后续 ${matches.count { it.order > match.order }} 盘。") },
        confirmButton = { Button(onClick = { onDelete(match.id, selectedRevision); deleting = null }, enabled = !busy) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deleting = null }, enabled = !busy) { Text("取消") } },
    ) }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    val date = matchDateText(match)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { ListItem(
        headlineContent = { Text("${if (match.outcome == Outcome.WIN) "胜" else "负"} · ${match.input?.rank?.let { "$it 段" } ?: "旧历史"}") },
        supportingContent = { Text("$date · ${String.format(Locale.US, "%+.1f", match.delta)}") },
        trailingContent = { Text(if (match.kind == MatchKind.NATIVE) "更正" else "只读") },
    ) }
}

private fun matchDateText(match: Match): String = match.playedAtEpochMs?.let { time -> match.playedZoneId?.let { zone ->
    runCatching { DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(time).atZone(ZoneId.of(zone))) }.getOrNull()
} } ?: "旧历史/日期未知"

private fun deleteDetail(match: Match): String {
    val result = if (match.outcome == Outcome.WIN) "胜" else "负"
    val opponent = match.input?.let { "${it.rank} 段 · 战绩 ${it.wins}-${it.losses}" } ?: "旧历史/对手信息未知"
    return "$result · $opponent · ${matchDateText(match)} · ${String.format(Locale.US, "%+.1f", match.delta)}"
}
