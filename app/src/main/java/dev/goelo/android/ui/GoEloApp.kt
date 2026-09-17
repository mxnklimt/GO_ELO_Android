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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.goelo.android.ui.components.*
import dev.goelo.android.ui.theme.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween

@Composable
fun GoEloApp(viewModel: AppViewModel, onSave: () -> Unit = {}, onShare: () -> Unit = {}, onChooseRestore: () -> Unit = {}, onSaveDestination: (android.net.Uri) -> Unit = {}) {
    val state by viewModel.ui.collectAsState()
    val snapshot = state.snapshot
    val profile = snapshot?.state?.profile
    if (snapshot == null) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Box(Modifier.safeDrawingPadding(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
        }
        return
    }
    if (profile == null) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Box(Modifier.safeDrawingPadding()) { OnboardingScreen(onCreate = viewModel::createProfile) }
        }
        return
    }
    Surface(Modifier.fillMaxSize(), color = Ink) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()
        .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF151C16), Ink, Ink)))) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            Crossfade(targetState = state.tab, animationSpec = tween(180), label = "page-transition") { tab ->
            when (tab) {
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
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.undo != null) TextButton(onClick = viewModel::undoLastRecord, enabled = !state.busy) { Text("撤销") }
            Button(onClick = viewModel::openRecord, enabled = !state.busy,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = MaterialTheme.shapes.medium) {
                AppMark(Mark.PLUS, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("记一盘")
            }
        }
        NavigationBar(containerColor = Ink, tonalElevation = 0.dp, windowInsets = WindowInsets(0,0,0,0)) {
            listOf(
                AppTab.GROWTH to "成长",
                AppTab.ANALYSIS to "分析",
                AppTab.HISTORY to "历史",
                AppTab.SETTINGS to "我的",
            ).forEach { (tab, label) ->
                NavigationBarItem(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    icon = { AppMark(when(tab) {
                        AppTab.GROWTH -> Mark.GROWTH
                        AppTab.ANALYSIS -> Mark.ANALYSIS
                        AppTab.HISTORY -> Mark.HISTORY
                        AppTab.SETTINGS -> Mark.PROFILE
                    }, color = if(state.tab == tab) Gold else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Gold, selectedTextColor = Gold,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
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
        onSave = onSave, onShare = onShare, onChooseRestore = onChooseRestore, onSaveCancelled = viewModel::clearPreparedBackup, onSaveDestination = onSaveDestination,
        onDismiss = viewModel::closeBackup,
    )
    state.restorePreview?.let { dev.goelo.android.ui.backup.RestorePreviewSheet(it, state.busy, viewModel::confirmRestore, viewModel::cancelRestore) }
}
