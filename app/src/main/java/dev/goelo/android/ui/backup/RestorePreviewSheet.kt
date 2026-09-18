package dev.goelo.android.ui.backup

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.backup.PreparedRestore
import dev.goelo.android.model.ratingOf
import dev.goelo.android.model.forPlayer
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePreviewSheet(prepared: PreparedRestore, busy: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    ModalBottomSheet(onDismissRequest={if(!busy) onCancel()},containerColor=Ink,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(18.dp)) {
            PageHeading("找回你的成长记录","RESTORE  /  确认恢复")
            val profile=prepared.envelope.state.profile
            val ledger=prepared.envelope.state
            val current=profile?.let { ledger.ratingOf(it.id) } ?: 0.0
            EloHero(profile?.name ?: "未创建档案",current,profile?.let { ledger.forPlayer(it.id).matches.size } ?: 0)
            Text("备份包含 ${ledger.allProfiles.size} 位棋手 · ${ledger.matches.size} 场对局",color=Gold)
            Text("来源：${prepared.sourceName}",style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("确认后将整体替换本机全部棋手和对局。",style=MaterialTheme.typography.bodyMedium)
            Button(onClick=onConfirm,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                shape=MaterialTheme.shapes.medium) { Text("确认恢复") }
            TextButton(onClick=onCancel,enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("取消") }
        }
    }
}
