package dev.goelo.android.ui.backup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goelo.android.backup.PreparedRestore
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RestorePreviewSheet(prepared: PreparedRestore, busy: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
 ModalBottomSheet(onDismissRequest=onCancel) { Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("确认恢复", style=MaterialTheme.typography.titleLarge); Text("来源：${prepared.sourceName}")
  val profile = prepared.envelope.state.profile
  val current = prepared.envelope.state.matches.maxByOrNull { it.order }?.eloAfter ?: profile?.initialElo ?: 0.0
  Text("档案：${profile?.name ?: "未创建"}"); Text("当前分：$current")
  Text("对局数：${prepared.envelope.state.matches.size}"); Text("整体替换当前数据")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick=onConfirm, enabled=!busy){Text("确认恢复")}; TextButton(onClick=onCancel, enabled=!busy){Text("取消")} }
  Spacer(Modifier.height(16.dp))
 } }
}
