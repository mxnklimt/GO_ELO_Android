package dev.goelo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dev.goelo.android.ui.AppViewModel
import dev.goelo.android.ui.GoEloApp
import dev.goelo.android.ui.theme.BlackGoldTheme
import java.time.Clock
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels { AppViewModelFactory((application as GoEloApplication).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri == null) appViewModel.clearPreparedBackup() else appViewModel.takePreparedBackup()?.let { bytes ->
                    lifecycleScope.launch { try { (application as GoEloApplication).container.documentGateway.write(uri, bytes); appViewModel.clearPreparedBackup() } catch (_: Throwable) { appViewModel.clearPreparedBackup() } }
                }
            }
            val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) lifecycleScope.launch { try { val bytes=(application as GoEloApplication).container.documentGateway.read(uri); appViewModel.previewRestore(bytes, "恢复文件") } catch (_: Throwable) {} }
            }
            BlackGoldTheme { GoEloApp(appViewModel, onSave = { appViewModel.prepareBackup(); save.launch("GO-ELO-backup.goelo.json") }, onChooseRestore = { open.launch(arrayOf("application/json", "*/*")) }, onShare = { appViewModel.takePreparedBackup()?.let { bytes -> lifecycleScope.launch { val uri=(application as GoEloApplication).container.documentGateway.createShareUri("GO-ELO-backup.json", bytes); startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="application/json"; putExtra(Intent.EXTRA_STREAM, uri); clipData=ClipData.newRawUri("GO ELO 备份", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享备份")) } } }) }
        }
    }
}

private class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(AppViewModel::class.java))
        return AppViewModel(
            store = container.stateStore,
            matches = container.matchService,
            profiles = container.profileService,
            clock = Clock.systemDefaultZone(),
            newId = { UUID.randomUUID().toString() },
            codec = container.backupCodec,
            restoreService = container.restoreService,
        ) as T
    }
}
