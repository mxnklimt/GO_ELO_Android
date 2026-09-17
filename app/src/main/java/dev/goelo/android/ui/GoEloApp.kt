package dev.goelo.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.goelo.android.model.Outcome
import dev.goelo.android.ui.analysis.AnalysisScreen
import dev.goelo.android.ui.growth.GrowthScreen
import dev.goelo.android.ui.history.HistoryScreen
import dev.goelo.android.ui.onboarding.OnboardingScreen
import dev.goelo.android.ui.record.RecordSheet
import dev.goelo.android.ui.settings.SettingsScreen
import dev.goelo.android.ui.backup.BackupSheet

@Composable
fun GoEloApp(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val snapshot = state.snapshot
    val profile = snapshot?.state?.profile
    if (profile == null || snapshot == null) {
        OnboardingScreen(onCreate = viewModel::createProfile)
        return
    }
    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            when (state.tab) {
                AppTab.GROWTH -> GrowthScreen(
                    state = snapshot.state,
                    range = state.range,
                    onRange = viewModel::selectRange,
                    onRecord = viewModel::openRecord,
                    onBackup = viewModel::openBackup,
                    undoAvailable = state.undo != null,
                    onUndo = viewModel::undoLastRecord,
                )
                AppTab.ANALYSIS -> AnalysisScreen(snapshot.state, state.range, viewModel::selectRange)
                AppTab.HISTORY -> HistoryScreen(
                    matches = snapshot.state.matches,
                    revision = snapshot.revision,
                    busy = state.busy,
                    error = state.error,
                    onEdit = viewModel::editMatch,
                    onDelete = viewModel::deleteMatch,
                )
                AppTab.SETTINGS -> SettingsScreen(
                    state = snapshot.state,
                    revision = snapshot.revision,
                    busy = state.busy,
                    error = state.error,
                    onRename = viewModel::renameProfile,
                    onInitialElo = viewModel::changeInitialElo,
                    onTarget = viewModel::setTarget,
                    onBackup = viewModel::openBackup,
                )
            }
        }
        NavigationBar {
            listOf(
                AppTab.GROWTH to "成长",
                AppTab.ANALYSIS to "分析",
                AppTab.HISTORY to "历史",
                AppTab.SETTINGS to "我的",
            ).forEach { (tab, label) ->
                NavigationBarItem(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    icon = {},
                    label = { Text(label) },
                )
            }
        }
    }
    if (state.recordOpen) RecordSheet(
        rank = state.rank,
        text = state.recordText,
        selfElo = snapshot.state.matches.maxByOrNull { it.order }?.eloAfter ?: profile.initialElo,
        busy = state.busy,
        error = state.error,
        onRank = viewModel::setRank,
        onText = viewModel::setRecordText,
        onSubmit = viewModel::submit,
        onDismiss = viewModel::closeRecord,
    )
    if (state.backupOpen) BackupSheet(
        busy = state.busy,
        onSave = viewModel::prepareBackup, onShare = {}, onChooseRestore = {}, onSaveCancelled = viewModel::clearPreparedBackup, onSaveDestination = {},
        onDismiss = viewModel::closeBackup,
    )
    state.restorePreview?.let { dev.goelo.android.ui.backup.RestorePreviewSheet(it, state.busy, viewModel::confirmRestore, viewModel::cancelRestore) }
}
