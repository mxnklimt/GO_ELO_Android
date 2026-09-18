package dev.goelo.android.ui.backup

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(busy: Boolean, onSave: () -> Unit, onShare: () -> Unit, onChooseRestore: () -> Unit,
    onSaveCancelled: () -> Unit, onSaveDestination: (android.net.Uri) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Ink,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(18.dp)) {
            AppMark(Mark.BACKUP,Modifier.size(36.dp))
            PageHeading("把成长，随身带走。","BACKUP  /  数据备份")
            Text("一份文件，保存全部棋手档案与对局。换机后选择文件，即可恢复。",
                style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick=onSave,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                shape=MaterialTheme.shapes.medium) { Text("保存文件") }
            OutlinedButton(onClick=onShare,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                shape=MaterialTheme.shapes.medium) { Text("分享备份") }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text("已有备份？",style=MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick=onChooseRestore,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),
                shape=MaterialTheme.shapes.medium) { Text("选择恢复文件") }
            TextButton(onClick=onDismiss,enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }
}
