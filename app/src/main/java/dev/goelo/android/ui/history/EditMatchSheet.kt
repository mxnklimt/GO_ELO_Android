package dev.goelo.android.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Match
import dev.goelo.android.model.Outcome
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
    affectedCount: Int,
    busy: Boolean,
    error: String?,
    onRank: (Int) -> Unit,
    onRecord: (String) -> Unit,
    onOutcome: (Outcome) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val input = parseRecord(rank, record).getOrNull()
    val self = match.eloBefore
    val known = match.opponentPlayerId != null
    val preview = (if(known) match.opponentElo else input?.let(::opponentElo))?.let { scoreChange(self,it,outcome) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("更正对局") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("原日期保持不变；保存后会同步更新双方及所有受影响的后续对局。", style = MaterialTheme.typography.bodySmall)
            if(known) Text("对手：${match.opponentName ?: "已有棋手"} · 赛前 ${match.opponentElo?.let { String.format(Locale.US,"%.1f",it) }}")
            else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..9).forEach { value -> FilterChip(value == rank, { onRank(value) }, enabled = !busy, label = { Text("$value 段") }) }
            }
            OutlinedTextField(record, onRecord, label = { Text("对手最近战绩，如 11-8") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(outcome == Outcome.WIN, { onOutcome(Outcome.WIN) }, label = { Text("胜") })
                FilterChip(outcome == Outcome.LOSS, { onOutcome(Outcome.LOSS) }, label = { Text("负") })
            }
            preview?.let { Text("预计 ${String.format(Locale.US, "%+.1f", it.delta)}", color = MaterialTheme.colorScheme.primary) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = onSave, enabled = (known || input != null) && !busy) { Text("保存更正") } },
        dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDelete, enabled = !busy) { Text("删除") }
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        } },
    )
}
