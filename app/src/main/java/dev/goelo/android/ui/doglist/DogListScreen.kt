package dev.goelo.android.ui.doglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.goelo.android.stats.filterDogIds
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*

@Composable
fun DogListScreen(
    ids: List<String>,
    revision: Long,
    busy: Boolean,
    error: String?,
    onAdd: (String, Long) -> Unit,
    onRemove: (String, Long) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val visible = filterDogIds(ids, search)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeading("狗榜", "DOG LIST  /  野狐 ID")
        PanelCard {
            Text("记录可疑账号", style = MaterialTheme.typography.titleMedium)
            Text("只保存野狐 ID，数据留在本机并随备份文件导出。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("野狐 ID") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).semantics { testTag = "dog-input" },
                )
                Button(
                    onClick = { onAdd(input.trim(), revision); input = "" },
                    enabled = !busy && input.trim().isNotEmpty(),
                    modifier = Modifier.semantics { testTag = "dog-add" },
                ) { Text("加入") }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        SectionTitle("已记录", "${ids.size} 个 ID")
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("检索野狐 ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { testTag = "dog-search" },
        )
        if (visible.isEmpty()) {
            EmptyPanel(
                if (ids.isEmpty()) "狗榜还是空的" else "没有匹配的 ID",
                if (ids.isEmpty()) "录入一个野狐 ID 后，它会出现在这里。" else "换一个关键词试试。",
                Mark.STONE,
            )
        } else {
            visible.forEach { id ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Panel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AppMark(Mark.STONE)
                        Text(id, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        TextButton(
                            onClick = { onRemove(id, revision) },
                            enabled = !busy,
                            modifier = Modifier.semantics { testTag = "dog-remove-$id" },
                        ) { Text("移除") }
                    }
                }
            }
        }
    }
}
