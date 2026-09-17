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

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels { AppViewModelFactory((application as GoEloApplication).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlackGoldTheme { GoEloApp(appViewModel) }
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
