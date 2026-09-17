package dev.goelo.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.AppState
import dev.goelo.android.model.MatchKind
import dev.goelo.android.stats.goalView
import java.util.Locale
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@Composable
fun SettingsScreen(
    state: AppState,
    revision: Long,
    busy: Boolean,
    error: String?,
    onRename: (String, Long) -> Unit,
    onInitialElo: (Double, Long) -> Unit,
    onTarget: (Double?, Long) -> Unit,
    onBackup: () -> Unit = {},
) {
    val profile = requireNotNull(state.profile)
    val current = state.matches.maxByOrNull { it.order }?.eloAfter ?: profile.initialElo
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var initial by remember(profile.initialElo) { mutableStateOf(String.format(Locale.US, "%.1f", profile.initialElo)) }
    var target by remember(profile.targetElo) { mutableStateOf(profile.targetElo?.let { String.format(Locale.US, "%.1f", it) } ?: "") }
    val hasLegacy = state.matches.any { it.kind == MatchKind.LEGACY }
    val goal = goalView(profile, current)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeading("我的棋力档案", "PROFILE  /  个人设置")
        EloHero(profile.name, current, state.matches.size)
        OutlinedButton(onClick = onBackup, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = MaterialTheme.shapes.medium) {
            AppMark(Mark.BACKUP)
            Spacer(Modifier.width(10.dp))
            Text("备份与恢复")
        }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("个人档案", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onRename(name, revision) }, enabled = name.trim().isNotEmpty() && !busy) { Text("保存名称") }
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("初始等级分", style = MaterialTheme.typography.titleMedium)
            Text("修改后会重算所有新记录。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(initial, { initial = it }, label = { Text("初始分") }, singleLine = true, enabled = !hasLegacy, modifier = Modifier.fillMaxWidth())
            Button(onClick = { initial.toDoubleOrNull()?.let { onInitialElo(it, revision) } }, enabled = !hasLegacy && initial.toDoubleOrNull() != null && !busy) { Text("重新计算") }
            if (hasLegacy) Text("旧历史的起点由原始分差和导入时的当前分推导，保持固定。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("成长目标", style = MaterialTheme.typography.titleMedium)
            Text("当前 ${String.format(Locale.US, "%.1f", current)} · ${goal.rankLabel}")
            OutlinedTextField(target, { target = it }, label = { Text("自定义目标（须高于当前分）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { target.toDoubleOrNull()?.let { onTarget(it, revision) } }, enabled = target.toDoubleOrNull()?.let { it > current } == true && !busy) { Text("设置目标") }
                TextButton(onClick = { onTarget(null, revision) }, enabled = !busy) { Text("恢复自动目标") }
            }
            Text(if (goal.achieved) "目标已达成" else "距目标 ${goal.remaining?.let { String.format(Locale.US, "%.1f", it) } ?: "—"}", style = MaterialTheme.typography.bodySmall)
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("计算说明", style = MaterialTheme.typography.titleMedium)
            Text("对手强度按所选段位基准分和最近战绩计算；每局采用 K=20 的 ELO 更新。", style = MaterialTheme.typography.bodySmall)
            Text("参考段位用于估算，不等于任何平台的正式段位。", style = MaterialTheme.typography.bodySmall)
        } }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
    }
}
