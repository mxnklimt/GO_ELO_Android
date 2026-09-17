package dev.goelo.android.ui.record

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Outcome
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.rating.scoreChange
import java.util.Locale

@Composable
fun RecordSheet(rank: Int, text: String, selfElo: Double, busy: Boolean, error: String?, onRank: (Int) -> Unit,
    onText: (String) -> Unit, onSubmit: (Outcome) -> Unit, onDismiss: () -> Unit) {
    val parsed = parseRecord(rank, text).getOrNull()
    val opponent = parsed?.let(::opponentElo)
    val win = opponent?.let { scoreChange(selfElo, it, Outcome.WIN) }
    val lose = opponent?.let { scoreChange(selfElo, it, Outcome.LOSS) }
    val canSubmit = parsed != null && !busy
    AlertDialog(
        modifier = Modifier.semantics { testTag = "record-sheet" },
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("记一盘") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("对手段位")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) { (1..9).forEach { value ->
                    FilterChip(selected = value == rank, onClick = { onRank(value) }, enabled = !busy, label = { Text(value.toString()) }, modifier = Modifier.heightIn(min = 48.dp))
                } }
                OutlinedTextField(text, onText, label = { Text("对手最近战绩，如 11-8") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { testTag = "opponent-record" })
                if (opponent != null) {
                    Text(String.format(Locale.US, "%.1f", opponent), modifier = Modifier.semantics { testTag = "opponent-elo" }, color = MaterialTheme.colorScheme.primary)
                    Text("胜 ${format(win!!.delta)} · 负 ${format(lose!!.delta)}", style = MaterialTheme.typography.bodySmall)
                } else Text("仅接受总盘数不超过 20 的 W-L 战绩", style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSubmit(Outcome.LOSS) }, enabled = canSubmit, modifier = Modifier.semantics { testTag = "submit-loss" }) { Text("负") }
            Button(onClick = { onSubmit(Outcome.WIN) }, enabled = canSubmit, modifier = Modifier.semantics { testTag = "submit-win" }) { Text("胜") }
        } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}

private fun format(value: Double): String = String.format(Locale.US, "%+.1f", value)
