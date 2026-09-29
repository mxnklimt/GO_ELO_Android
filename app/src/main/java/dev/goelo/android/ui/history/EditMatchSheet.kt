package dev.goelo.android.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Match
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.rating.scoreChange
import java.util.Locale

@Composable
fun EditMatchSheet(
    match: Match,
    rank: Int,
    record: String,
    outcome: Outcome,
    candidates: List<Profile>,
    useKnownOpponent: Boolean,
    selectedOpponentId: String?,
    affectedCount: Int,
    busy: Boolean,
    error: String?,
    onRank: (Int) -> Unit,
    onRecord: (String) -> Unit,
    onOutcome: (Outcome) -> Unit,
    onMode: (Boolean) -> Unit,
    onOpponent: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember(match.id) { mutableStateOf("") }
    val input = if (useKnownOpponent) null else parseRecord(rank, record).getOrNull()
    val self = match.eloBefore
    val known = match.opponentPlayerId != null
    val preview = (if (known) match.opponentElo else if (useKnownOpponent) null else input?.let(::opponentElo))
        ?.let { scoreChange(self, it, outcome) }
    val canSave = known || if (useKnownOpponent) candidates.any { it.id == selectedOpponentId } else input != null
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("更正对局") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("原日期保持不变；保存后会重算${if (known || useKnownOpponent) "双方" else "当前棋手"}及受影响的后续对局（当前棋手后续 $affectedCount 盘）。", style = MaterialTheme.typography.bodySmall)
            if(known) Text("对手：${match.opponentName ?: "已有棋手"} · 赛前 ${match.opponentElo?.let { String.format(Locale.US,"%.1f",it) }}")
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!useKnownOpponent, { onMode(false) }, enabled = !busy, label = { Text("临时对手") })
                    FilterChip(useKnownOpponent, { onMode(true) }, enabled = !busy, label = { Text("改为已有棋手") })
                }
                if (useKnownOpponent) {
                    if (candidates.isEmpty()) Text("棋手库中没有其他棋手，请先添加棋手。")
                    else {
                        OutlinedTextField(search, { search = it }, label = { Text("搜索棋手姓名") },
                            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                        val visible = candidates.filter { it.name.contains(search.trim(), ignoreCase = true) }
                        if (visible.isEmpty()) Text("没有匹配的棋手")
                        Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            visible.forEach { player ->
                                val duplicate = candidates.count { it.name == player.name } > 1
                                FilterChip(selected = selectedOpponentId == player.id,
                                    onClick = { onOpponent(player.id) }, enabled = !busy,
                                    label = { Text(player.name + if (duplicate) " · ${player.id}" else "") },
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Text("将按该棋手在本局之前的等级分重新计算。", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..9).forEach { value -> FilterChip(value == rank, { onRank(value) }, enabled = !busy, label = { Text("$value 段") }) }
                    }
                    OutlinedTextField(record, onRecord, label = { Text("对手最近战绩，如 11-8") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(outcome == Outcome.WIN, { onOutcome(Outcome.WIN) }, enabled = !busy, label = { Text("胜") })
                FilterChip(outcome == Outcome.LOSS, { onOutcome(Outcome.LOSS) }, enabled = !busy, label = { Text("负") })
            }
            preview?.let { Text("预计 ${String.format(Locale.US, "%+.1f", it.delta)}", color = MaterialTheme.colorScheme.primary) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = onSave, enabled = canSave && !busy) { Text("保存更正") } },
        dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDelete, enabled = !busy) { Text("删除") }
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        } },
    )
}
