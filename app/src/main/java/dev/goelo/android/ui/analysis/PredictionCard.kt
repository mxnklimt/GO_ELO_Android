package dev.goelo.android.ui.analysis

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.model.ratingOf
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.stats.predictAgainst
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import java.util.Locale

private enum class PredictionMode { TEMPORARY, PLAYER_LIBRARY }

@Composable
fun PredictionCard(selfElo: Double, ledger: AppState, currentPlayerId: String) {
    var mode by remember { mutableStateOf(PredictionMode.TEMPORARY) }
    var rank by remember { mutableIntStateOf(7) }
    var record by remember { mutableStateOf("") }
    var opponentId by remember { mutableStateOf<String?>(null) }
    val candidates = ledger.allProfiles.filter { it.id != currentPlayerId }
    val parsed = if (record.isBlank()) null else parseRecord(rank, record)
    val opponent = when (mode) {
        PredictionMode.TEMPORARY -> parsed?.getOrNull()?.let(::opponentElo)
        PredictionMode.PLAYER_LIBRARY -> opponentId?.let { id -> candidates.find { it.id == id }?.let { ledger.ratingOf(it.id) } }
    }
    val prediction = opponent?.let { predictAgainst(selfElo, it) }

    PanelCard(Modifier.semantics { testTag = "prediction-card" }) {
        SectionTitle("胜率预测", "实时估算")
        Text("当前棋手 · ${eloText(selfElo)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == PredictionMode.TEMPORARY, { mode = PredictionMode.TEMPORARY }, label = { Text("临时对手") })
            FilterChip(mode == PredictionMode.PLAYER_LIBRARY, { mode = PredictionMode.PLAYER_LIBRARY }, label = { Text("棋手库") })
        }
        if (mode == PredictionMode.TEMPORARY) {
            Text("对手段位", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..9).forEach { value -> FilterChip(rank == value, { rank = value }, label = { Text("$value 段") }) }
            }
            OutlinedTextField(
                value = record, onValueChange = { record = it }, label = { Text("最近战绩，如 11-8") },
                supportingText = { Text("最近不超过 20 盘；未知战绩可输入 0-0") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { testTag = "prediction-record" },
            )
            if (parsed?.isFailure == true) Text(parsed.exceptionOrNull()?.message ?: "战绩格式无效",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else if (candidates.isEmpty()) {
            Text("暂无其他棋手", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("选择对手", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.fillMaxWidth().heightIn(max = 180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                candidates.forEach { player ->
                    FilterChip(
                        selected = opponentId == player.id, onClick = { opponentId = player.id },
                        label = { Text("${player.name} · ${eloText(ledger.ratingOf(player.id))}") },
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "prediction-opponent-${player.id}" },
                    )
                }
            }
        }
        if (prediction == null) {
            Text("输入对手信息后显示预测结果", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("对手等级分 · ${eloText(prediction.opponentElo)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PredictionNumber("你的胜率", String.format(Locale.US, "%.1f%%", prediction.winProbability * 100), Positive, Modifier.weight(1f))
                PredictionNumber("对手胜率", String.format(Locale.US, "%.1f%%", (1.0 - prediction.winProbability) * 100), Negative, Modifier.weight(1f))
            }
            Text("你胜 ${deltaText(prediction.winDelta)} · 你负 ${deltaText(prediction.lossDelta)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PredictionNumber(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
