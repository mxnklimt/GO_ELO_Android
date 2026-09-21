package dev.goelo.android.ui.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import dev.goelo.android.rating.rankBaseline

@Composable fun OnboardingScreen(onCreate: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var eloText by remember { mutableStateOf("2300") }
    val valid=name.trim().isNotEmpty() && eloText.toDoubleOrNull()?.let { it.isFinite() && it>0 }==true
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement=Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(18.dp))
        AppMark(Mark.STONE,Modifier.size(48.dp))
        Text("GO ELO",style=MaterialTheme.typography.displaySmall,color=Gold)
        PageHeading("让每一盘\n留下成长的轨迹。","个人棋力档案")
        PanelCard {
            Text("你的起点",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(name,{name=it},label={Text("棋手名称")},modifier=Modifier.fillMaxWidth().semantics { testTag="profile-name" },singleLine=true)
            Text("用段位快速设定起点",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(1,5,7,9).forEach { rank ->
                    AssistChip(onClick={eloText=rankBaseline(rank).toString()},label={Text("${rank}段")})
                }
            }
            OutlinedTextField(eloText,{eloText=it},label={Text("初始等级分")},
                modifier=Modifier.fillMaxWidth().semantics { testTag="initial-elo" },singleLine=true)
        }
        Button(onClick={onCreate(name.trim(),eloText.toDouble())},enabled=valid,
            modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=MaterialTheme.shapes.medium) {
            Text("开始记录")
        }
        Text("已有备份？创建档案后可在「我的」中恢复。",style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("离线使用  ·  本机保存  ·  自由备份",style=MaterialTheme.typography.labelMedium,color=MutedGold)
    }
}
