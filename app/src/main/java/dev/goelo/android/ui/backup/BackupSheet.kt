package dev.goelo.android.ui.backup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(busy: Boolean, onSave: () -> Unit, onShare: () -> Unit, onChooseRestore: () -> Unit,
    onSaveCancelled: () -> Unit, onSaveDestination: (android.net.Uri) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("数据备份", style = MaterialTheme.typography.titleLarge)
            Text("备份文件包含档案与全部对局，可用于换机恢复。")
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存文件") }
            OutlinedButton(onClick = onShare, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("分享备份") }
            OutlinedButton(onClick = onChooseRestore, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("选择恢复文件") }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
