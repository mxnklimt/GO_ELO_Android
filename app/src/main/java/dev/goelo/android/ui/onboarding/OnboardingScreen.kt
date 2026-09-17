package dev.goelo.android.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OnboardingScreen(onCreate: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var eloText by remember { mutableStateOf("2200") }
    val valid = name.trim().isNotEmpty() && eloText.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(36.dp))
        Text("GO ELO", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("离线记录你的围棋成长", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, label = { Text("棋手名称") }, modifier = Modifier.fillMaxWidth().semantics { testTag = "profile-name" }, singleLine = true)
        Text("用段位快速设定起点")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1, 5, 7, 9).forEach { rank ->
                AssistChip(onClick = { eloText = (1000 + 200 * (rank - 1)).toString() }, label = { Text("${rank}段") })
            }
        }
        OutlinedTextField(eloText, { eloText = it }, label = { Text("初始等级分") }, modifier = Modifier.fillMaxWidth().semantics { testTag = "initial-elo" }, singleLine = true)
        Button(onClick = { onCreate(name.trim(), eloText.toDouble()) }, enabled = valid, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("开始记录") }
        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("导入旧数据（即将开放）") }
        Text("数据只保存在本机，可随时导出备份。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
