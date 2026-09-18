package dev.goelo.android.ui.players

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.*
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersSheet(state: AppState, busy: Boolean, error: String?,
    onAdd: (String, Double) -> Unit, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("2000") }
    var playerCount by remember { mutableIntStateOf(state.allProfiles.size) }
    LaunchedEffect(state.allProfiles.size) {
        if (state.allProfiles.size > playerCount) { adding=false; name=""; search="" }
        playerCount=state.allProfiles.size
    }
    val visible=state.allProfiles.filter { it.name.contains(search.trim(), ignoreCase=true) }
        .sortedWith(compareByDescending<Profile> { it.id==state.profile?.id }.thenBy { it.name }.thenBy { it.id })
    ModalBottomSheet(onDismissRequest={if(!busy)onDismiss()},containerColor=Ink,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).imePadding().padding(horizontal=20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PageHeading("每位棋手，都有轨迹","PLAYERS  /  ${state.allProfiles.size} 位棋手")
            if(error!=null) Text(error,color=MaterialTheme.colorScheme.error)
            if(adding) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name,{name=it},enabled=!busy,singleLine=true,label={Text("棋手名称")},
                        modifier=Modifier.fillMaxWidth().semantics { testTag="add-player-name" })
                    OutlinedTextField(initial,{initial=it},enabled=!busy,singleLine=true,label={Text("初始等级分")},
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                        modifier=Modifier.fillMaxWidth().semantics { testTag="add-player-elo" })
                    Text("可直接填写当前等级分，或用段位基准填入。",style=MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        (1..9).forEach { rank -> AssistChip(onClick={initial=(1000+200*(rank-1)).toString()},
                            enabled=!busy,label={Text("$rank 段")}) }
                    }
                    Text("新棋手从此分数开始记录；已有棋手对局会同时更新双方。",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick={initial.toDoubleOrNull()?.let { onAdd(name,it) }},
                        enabled=!busy && name.isNotBlank() && initial.toDoubleOrNull()?.let { it.isFinite() && it>0 }==true,
                        modifier=Modifier.fillMaxWidth().semantics { testTag="add-player-save" }) { Text("保存棋手") }
                    TextButton(onClick={adding=false},enabled=!busy) { Text("返回棋手列表") }
                }
            } else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(search,{search=it},singleLine=true,label={Text("搜索棋手")},modifier=Modifier.weight(1f))
                    FilledTonalButton(onClick={adding=true},enabled=!busy,modifier=Modifier.align(androidx.compose.ui.Alignment.CenterVertically)) { Text("添加棋手") }
                }
                if(visible.isEmpty()) Text("没有匹配的棋手",color=MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f).semantics { testTag="players-list" },verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(visible,key={it.id}) { player ->
                        val games=state.matches.filter { it.playerId==player.id || it.opponentPlayerId==player.id }
                        val wins=games.count { (it.playerId==player.id && it.outcome==Outcome.WIN) || (it.opponentPlayerId==player.id && it.outcome==Outcome.LOSS) }
                        Surface(onClick={onSelect(player.id)},enabled=!busy,shape=MaterialTheme.shapes.large,color=Panel,
                            border=BorderStroke(1.dp,if(player.id==state.profile?.id) Gold else MaterialTheme.colorScheme.outlineVariant),
                            modifier=Modifier.fillMaxWidth().semantics { testTag="select-player-${player.id}" }) {
                            Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),
                                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                AppMark(Mark.PROFILE)
                                Column(Modifier.weight(1f)) {
                                    Text(player.name,style=MaterialTheme.typography.titleMedium)
                                    Text("${games.size} 盘 · $wins 胜 · ${games.size-wins} 负",style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    if(state.allProfiles.count { it.name==player.name }>1) Text("编号 ${player.id}",style=MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment=androidx.compose.ui.Alignment.End) {
                                    Text(eloText(state.ratingOf(player.id)),style=MaterialTheme.typography.titleLarge,color=Gold)
                                    Text(if(player.id==state.profile?.id) "当前棋手" else "查看档案",style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick=onDismiss,enabled=!busy,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) { Text("完成") }
        }
    }
}
