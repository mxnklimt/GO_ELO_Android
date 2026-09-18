package dev.goelo.android.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Match
import dev.goelo.android.model.Outcome
import dev.goelo.android.stats.HistoryRange
import dev.goelo.android.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Mark { GROWTH, ANALYSIS, HISTORY, PROFILE, BACKUP, PLUS, STONE }

@Composable fun AppMark(mark: Mark, modifier: Modifier = Modifier, color: Color = Gold) {
    Canvas(modifier.size(24.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot=Offset.Zero) {
            fun line(x: Float, y: Float, x2: Float, y2: Float) =
                drawLine(color, Offset(x,y), Offset(x2,y2), strokeWidth=1.7f, cap=StrokeCap.Round)
            when(mark) {
                Mark.GROWTH -> { line(4f,19f,4f,5f); line(4f,19f,21f,19f); line(7f,15f,12f,10f); line(12f,10f,15f,12f); line(15f,12f,21f,5f); line(17f,5f,21f,5f); line(21f,5f,21f,9f) }
                Mark.ANALYSIS -> { line(5f,18f,5f,12f); line(12f,18f,12f,5f); line(19f,18f,19f,9f) }
                Mark.HISTORY -> { drawRoundRect(color, Offset(5f,3f), Size(14f,18f), androidx.compose.ui.geometry.CornerRadius(2f), style=Stroke(1.7f)); line(8f,8f,16f,8f); line(8f,12f,16f,12f); line(8f,16f,13f,16f) }
                Mark.PROFILE -> { drawCircle(color,3.8f,Offset(12f,7f),style=Stroke(1.7f)); drawArc(color,180f,180f,false,Offset(4f,14f),Size(16f,12f),style=Stroke(1.7f)) }
                Mark.BACKUP -> { line(12f,3f,12f,14f); line(8f,10f,12f,14f); line(16f,10f,12f,14f); line(4f,14f,4f,20f); line(4f,20f,20f,20f); line(20f,20f,20f,14f) }
                Mark.PLUS -> { line(12f,5f,12f,19f); line(5f,12f,19f,12f) }
                Mark.STONE -> { drawCircle(color,8f,Offset(12f,12f),style=Stroke(1.7f)); drawCircle(color.copy(alpha=.25f),5f,Offset(12f,12f)); drawCircle(color,1.5f,Offset(9f,9f)) }
            }
        }
    }
}
@Composable fun PageHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(subtitle,style=MaterialTheme.typography.labelMedium,color=MutedGold)
            Spacer(Modifier.height(5.dp))
            Text(title,style=MaterialTheme.typography.headlineMedium)
        }
        action?.invoke()
    }
}
@Composable fun SectionTitle(title: String, note: String? = null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(3.dp,16.dp).clip(CircleShape).background(Gold))
        Spacer(Modifier.width(8.dp))
        Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
        if(note!=null) Text(note,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=Panel,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.65f))) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun RangeTabs(range: HistoryRange, onRange: (HistoryRange) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink).padding(4.dp),
        horizontalArrangement=Arrangement.spacedBy(3.dp)) {
        HistoryRange.entries.forEach { item ->
            Surface(onClick={onRange(item)},modifier=Modifier.weight(1f).heightIn(min=44.dp).semantics { selected=item==range },
                shape=RoundedCornerShape(10.dp),
                color=if(item==range) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor=if(item==range) Gold else MaterialTheme.colorScheme.onSurfaceVariant) {
                Box(Modifier.padding(vertical=10.dp),contentAlignment=Alignment.Center) {
                    Text(if(item==HistoryRange.ALL) "全部" else "${item.count}盘",style=MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
@Composable fun EmptyPanel(title: String, detail: String, mark: Mark = Mark.STONE) {
    PanelCard {
        AppMark(mark,Modifier.size(30.dp),MutedGold)
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
fun eloText(value: Double): String = String.format(Locale.US,"%.1f",value)
fun deltaText(value: Double): String = String.format(Locale.US,"%+.1f",value)

@Composable fun ResultRow(match: Match, onClick: (() -> Unit)? = null) {
    val win=match.outcome==Outcome.WIN
    val tint=if(win) Positive else Negative
    val opponent=match.opponentName ?: match.input?.let { "${it.rank} 段 · ${it.wins}-${it.losses}" }
        ?: match.legacy?.let { if(it.selfSide==1) it.player2 else it.player1 } ?: "旧历史"
    val date=match.playedAtEpochMs?.let { epoch ->
        runCatching { DateTimeFormatter.ofPattern("MM.dd").format(Instant.ofEpochMilli(epoch).atZone(ZoneId.of(match.playedZoneId ?: "UTC"))) }.getOrNull()
    } ?: "日期未知"
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)
        .then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha=.10f))
            .border(1.dp,tint.copy(alpha=.18f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
            Text(if(win) "胜" else "负",style=MaterialTheme.typography.titleMedium,color=tint)
        }
        Column(Modifier.weight(1f)) {
            Text(opponent,style=MaterialTheme.typography.titleSmall)
            Text("对局 #${match.order} · $date",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment=Alignment.End) {
            Text(deltaText(match.delta),style=MaterialTheme.typography.titleMedium,color=tint)
            Text(eloText(match.eloAfter),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(onClick!=null) Text(if(match.kind==dev.goelo.android.model.MatchKind.NATIVE) "更正" else "只读",
                style=MaterialTheme.typography.labelSmall,color=MutedGold)
        }
    }
}
