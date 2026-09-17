package dev.goelo.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.StateStore
import dev.goelo.android.model.Outcome
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.data.UndoToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Clock

class AppViewModel(
    private val store: StateStore,
    private val matches: MatchService,
    private val profiles: ProfileService,
    private val clock: Clock,
    private val newId: () -> String,
) : ViewModel() {
    private val mutableUi = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = mutableUi.asStateFlow()
    private var pendingRequest: PendingRequest? = null

    init {
        viewModelScope.launch {
            store.observe().collect { snapshot ->
                mutableUi.value = mutableUi.value.copy(snapshot = snapshot)
            }
        }
    }

    fun createProfile(name: String, initialElo: Double) = mutate {
        profiles.create(name, initialElo)
    }

    fun selectTab(tab: AppTab) { mutableUi.value = mutableUi.value.copy(tab = tab) }
    fun selectRange(range: dev.goelo.android.stats.HistoryRange) { mutableUi.value = mutableUi.value.copy(range = range) }

    fun openRecord() {
        val rank = mutableUi.value.snapshot?.state?.profile?.lastOpponentRank ?: 7
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(recordOpen = true, rank = rank, recordText = "", error = null)
    }

    fun closeRecord() {
        if (!mutableUi.value.busy) mutableUi.value = mutableUi.value.copy(recordOpen = false, error = null)
    }

    fun setRank(rank: Int) {
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(rank = rank.coerceIn(1, 9), error = null)
    }

    fun setRecordText(text: String) {
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(recordText = text, error = null)
    }

    fun submit(outcome: Outcome) {
        val state = mutableUi.value
        val input = parseRecord(state.rank, state.recordText).getOrElse {
            mutableUi.value = state.copy(error = "请输入不超过 20 盘的战绩，例如 11-8")
            return
        }
        val request = pendingRequest?.takeIf { it.input == input && it.outcome == outcome }
            ?: PendingRequest(newId(), input, outcome, clock.instant().toEpochMilli(), clock.zone.id).also { pendingRequest = it }
        mutate(onSuccess = {
            pendingRequest = null
            mutableUi.value = mutableUi.value.copy(recordOpen = false, recordText = "", undo = it, error = null)
        }) {
            matches.record(request.id, request.input, request.outcome, request.atEpochMs, request.zoneId).undo
        }
    }

    fun undoLastRecord() {
        val token = mutableUi.value.undo ?: return
        mutate(onSuccess = { mutableUi.value = mutableUi.value.copy(undo = null) }) { matches.undo(token); null }
    }

    private fun mutate(onSuccess: (UndoToken?) -> Unit = {}, block: suspend () -> UndoToken?) {
        if (mutableUi.value.busy) return
        mutableUi.value = mutableUi.value.copy(busy = true, error = null, undo = null)
        viewModelScope.launch {
            try {
                onSuccess(block())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                mutableUi.value = mutableUi.value.copy(error = "保存失败，记录未改变")
            } finally {
                mutableUi.value = mutableUi.value.copy(busy = false)
            }
        }
    }

    private data class PendingRequest(
        val id: String,
        val input: dev.goelo.android.model.RecordInput,
        val outcome: Outcome,
        val atEpochMs: Long,
        val zoneId: String,
    )
}
