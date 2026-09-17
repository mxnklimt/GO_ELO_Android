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
import java.time.ZoneId

class AppViewModel(
    private val store: StateStore,
    private val matches: MatchService,
    private val profiles: ProfileService,
    private val clock: Clock,
    private val newId: () -> String,
) : ViewModel() {
    private val mutableUi = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = mutableUi.asStateFlow()
    private var pendingRequestId: String? = null

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
        pendingRequestId = null
        mutableUi.value = mutableUi.value.copy(recordOpen = true, rank = rank, recordText = "", error = null)
    }

    fun closeRecord() {
        if (!mutableUi.value.busy) mutableUi.value = mutableUi.value.copy(recordOpen = false, error = null)
    }

    fun setRank(rank: Int) {
        pendingRequestId = null
        mutableUi.value = mutableUi.value.copy(rank = rank.coerceIn(1, 9), error = null)
    }

    fun setRecordText(text: String) {
        pendingRequestId = null
        mutableUi.value = mutableUi.value.copy(recordText = text, error = null)
    }

    fun submit(outcome: Outcome) {
        val state = mutableUi.value
        val input = parseRecord(state.rank, state.recordText).getOrElse {
            mutableUi.value = state.copy(error = "请输入不超过 20 盘的战绩，例如 11-8")
            return
        }
        val requestId = pendingRequestId ?: newId().also { pendingRequestId = it }
        mutate(onSuccess = {
            pendingRequestId = null
            mutableUi.value = mutableUi.value.copy(recordOpen = false, recordText = "", undo = it, error = null)
        }) {
            val instant = clock.instant()
            matches.record(requestId, input, outcome, instant.toEpochMilli(), ZoneId.systemDefault().id).undo
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
}
