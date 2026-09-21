package dev.goelo.android.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Outcome
import dev.goelo.android.stats.HeadToHeadStat
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HeadToHeadCard(stats: List<HeadToHeadStat>) {
    SectionTitle("棋手对局", "双方均在棋手库")
    if (stats.isEmpty()) {
        EmptyPanel("暂无棋手库对局", "使用“已有棋手”模式记录对局后，这里会显示双方历史胜负。", Mark.PROFILE)
        return
    }
    var expandedOpponent by remember { mutableStateOf<String?>(null) }
    PanelCard {
        stats.forEach { stat ->
            val expanded = expandedOpponent == stat.opponentId
            Surface(
                onClick = { expandedOpponent = if (expanded) null else stat.opponentId },
                shape = MaterialTheme.shapes.medium,
                color = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else Panel,
                modifier = Modifier.fillMaxWidth().semantics { testTag = "head-to-head-${stat.opponentId}" },
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${stat.opponentName} · ${stat.wins}胜 ${stat.losses}负", style = MaterialTheme.typography.titleSmall)
                            Text("${stat.games}盘", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(String.format(Locale.US, "%.1f%%", stat.winRate * 100),
                            style = MaterialTheme.typography.titleMedium, color = if (stat.winRate >= .5) Positive else Negative)
                    }
                    if (expanded) {
                        stat.matches.forEach { match ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (match.outcome == Outcome.WIN) "胜" else "负",
                                    color = if (match.outcome == Outcome.WIN) Positive else Negative,
                                    style = MaterialTheme.typography.labelMedium)
                                Text("第 ${match.order} 盘 · ${matchDate(match.playedAtEpochMs, match.playedZoneId)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun matchDate(epoch: Long?, zoneId: String?): String = if (epoch == null || zoneId == null) "日期未知" else
    runCatching { DateTimeFormatter.ofPattern("yyyy.MM.dd").format(Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId))) }
        .getOrDefault("日期未知")
