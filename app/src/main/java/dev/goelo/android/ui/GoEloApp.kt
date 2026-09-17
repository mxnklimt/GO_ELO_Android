package dev.goelo.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goelo.android.model.Outcome
import dev.goelo.android.ui.growth.GrowthScreen
import dev.goelo.android.ui.onboarding.OnboardingScreen
import dev.goelo.android.ui.record.RecordSheet

@Composable
fun GoEloApp(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val snapshot = state.snapshot
    val profile = snapshot?.state?.profile
    if (profile == null || snapshot == null) {
        OnboardingScreen(onCreate = viewModel::createProfile)
        return
    }
    GrowthScreen(
        state = snapshot.state,
        range = state.range,
        onRange = viewModel::selectRange,
        onRecord = viewModel::openRecord,
        onBackup = {},
        undoAvailable = state.undo != null,
        onUndo = viewModel::undoLastRecord,
    )
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
}
