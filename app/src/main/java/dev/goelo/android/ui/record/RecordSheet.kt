package dev.goelo.android.ui.record

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.goelo.android.model.Outcome
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.rating.scoreChange
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(rank: Int, text: String, selfElo: Double, busy: Boolean, error: String?, onRank: (Int) -> Unit,
    onText: (String) -> Unit, onSubmit: (Outcome) -> Unit, onDismiss: () -> Unit) {
    val parsed=parseRecord(rank,text).getOrNull()
    val opponent=parsed?.let(::opponentElo)
    val win=opponent?.let { scoreChange(selfElo,it,Outcome.WIN) }
    val lose=opponent?.let { scoreChange(selfElo,it,Outcome.LOSS) }
    val canSubmit=parsed!=null && !busy
    ModalBottomSheet(onDismissRequest={if(!busy) onDismiss()},containerColor=Ink,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        modifier=Modifier.semantics { testTag="record-sheet" }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start=24.dp,end=24.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            PageHeading("记下这一盘","RECORD  /  对局记录")
            SectionTitle("对手段位")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                (1..9).forEach { value -> FilterChip(selected=value==rank,onClick={onRank(value)},enabled=!busy,
                    label={Text("$value 段")},modifier=Modifier.heightIn(min=48.dp)) }
            }
            OutlinedTextField(text,onText,label={Text("对手最近战绩，如 11-8")},singleLine=true,enabled=!busy,
                supportingText={Text("最近不超过 20 盘；未知战绩可输入 0-0")},
                modifier=Modifier.fillMaxWidth().semantics { testTag="opponent-record" })
            PanelCard {
                Text("对手估算等级分",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(opponent?.let(::eloText) ?: "—",style=MaterialTheme.typography.displaySmall,color=Gold,
                    modifier=Modifier.semantics { testTag="opponent-elo" })
                Text("你的赛前等级分  ${eloText(selfElo)}",style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(error!=null) Text(error,color=MaterialTheme.colorScheme.error)
            Text("本局结果",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={onSubmit(Outcome.LOSS)},enabled=canSubmit,
                    modifier=Modifier.weight(1f).heightIn(min=68.dp).semantics { testTag="submit-loss" },
                    shape=MaterialTheme.shapes.medium,colors=ButtonDefaults.outlinedButtonColors(contentColor=Negative)) {
                    Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("负",style=MaterialTheme.typography.titleMedium)
                        Text(lose?.let { deltaText(it.delta) } ?: "—",style=MaterialTheme.typography.labelMedium)
                    }
                }
                Button(onClick={onSubmit(Outcome.WIN)},enabled=canSubmit,
                    modifier=Modifier.weight(1f).heightIn(min=68.dp).semantics { testTag="submit-win" },
                    shape=MaterialTheme.shapes.medium) {
                    Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("胜",style=MaterialTheme.typography.titleMedium)
                        Text(win?.let { deltaText(it.delta) } ?: "—",style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
            TextButton(onClick=onDismiss,enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("取消") }
        }
    }
}
