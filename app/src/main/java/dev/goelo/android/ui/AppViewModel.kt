package dev.goelo.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.StateStore
import dev.goelo.android.model.Outcome
import dev.goelo.android.rating.parseRecord
import dev.goelo.android.data.UndoToken
import dev.goelo.android.model.RecordInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Clock
import dev.goelo.android.backup.BackupCodec
import dev.goelo.android.backup.BackupEnvelope
import dev.goelo.android.backup.PreparedRestore
import dev.goelo.android.backup.RestoreService

class AppViewModel(
    private val store: StateStore,
    private val matches: MatchService,
    private val profiles: ProfileService,
    private val clock: Clock,
    private val newId: () -> String,
    private val codec: BackupCodec? = null,
    private val restoreService: RestoreService? = null,
) : ViewModel() {
    private val mutableUi = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = mutableUi.asStateFlow()
    private var pendingRequest: PendingRequest? = null
    private var exportBytes: ByteArray? = null

    init {
        viewModelScope.launch {
            store.observe().collect { snapshot ->
                mutableUi.value = mutableUi.value.copy(snapshot = snapshot,
                    undo = mutableUi.value.undo?.takeIf { it.revision == snapshot.revision })
            }
        }
    }

    fun createProfile(name: String, initialElo: Double) = mutate {
        profiles.create(name, initialElo)
        null
    }

    fun selectTab(tab: AppTab) { mutableUi.value = mutableUi.value.copy(tab = tab) }
    fun openPlayers() { if (!mutableUi.value.busy) mutableUi.value = mutableUi.value.copy(playersOpen = true, error = null) }
    fun closePlayers() { if (!mutableUi.value.busy) mutableUi.value = mutableUi.value.copy(playersOpen = false, error = null) }
    fun addPlayer(name: String, elo: Double) {
        val snapshot = mutableUi.value.snapshot ?: return
        mutate { profiles.add(newId(), name, elo, snapshot.revision); null }
    }
    fun selectPlayer(id: String) {
        val snapshot = mutableUi.value.snapshot ?: return
        mutate(onSuccess = {
            pendingRequest = null
            mutableUi.value = mutableUi.value.copy(playersOpen = false, recordOpen = false, opponentId = null, undo = null)
        }) {
            val selected = profiles.select(id, snapshot.revision)
            mutableUi.value = mutableUi.value.copy(snapshot = selected)
            null
        }
    }
    fun setKnownOpponentMode(known: Boolean) {
        if (mutableUi.value.busy) return
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(knownOpponentMode = known, opponentId = null, error = null)
    }
    fun setOpponent(id: String) {
        if (mutableUi.value.busy) return
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(opponentId = id, error = null)
    }
    fun openBackup() { mutableUi.value = mutableUi.value.copy(backupOpen = true, error = null) }
    fun closeBackup() { mutableUi.value = mutableUi.value.copy(backupOpen = false) }
    fun prepareBackup(onReady: (() -> Unit)? = null) {
        val current = mutableUi.value.snapshot ?: return
        val c = codec ?: return
        viewModelScope.launch {
            mutableUi.value = mutableUi.value.copy(busy = true, error = null)
            try {
                exportBytes = c.encode(BackupEnvelope(exportedAtEpochMs = clock.millis(), appVersion = "0.3.0", state = current.state))
                mutableUi.value = mutableUi.value.copy(pendingExport = true)
                onReady?.invoke()
            } catch (e: Throwable) { mutableUi.value = mutableUi.value.copy(error = e.message ?: "备份失败") }
            finally { mutableUi.value = mutableUi.value.copy(busy = false) }
        }
    }
    fun takePreparedBackup(): ByteArray? = exportBytes
    fun clearPreparedBackup() { exportBytes = null; mutableUi.value = mutableUi.value.copy(pendingExport = false) }
    fun previewRestore(bytes: ByteArray, sourceName: String) {
        val service = restoreService ?: return
        viewModelScope.launch {
            mutableUi.value = mutableUi.value.copy(busy = true, error = null)
            try { mutableUi.value = mutableUi.value.copy(restorePreview = service.prepare(bytes, sourceName)) }
            catch (e: Throwable) { mutableUi.value = mutableUi.value.copy(error = e.message ?: "恢复文件不可读") }
            finally { mutableUi.value = mutableUi.value.copy(busy = false) }
        }
    }
    fun cancelRestore() { mutableUi.value = mutableUi.value.copy(restorePreview = null) }
    fun confirmRestore() {
        val service = restoreService ?: return
        val prepared = mutableUi.value.restorePreview ?: return
        viewModelScope.launch {
            mutableUi.value = mutableUi.value.copy(busy = true, error = null)
            try { service.apply(prepared); pendingRequest = null; mutableUi.value = mutableUi.value.copy(restorePreview = null, backupOpen = false, undo = null, recordOpen = false, playersOpen = false) }
            catch (e: Throwable) { mutableUi.value = mutableUi.value.copy(error = e.message ?: "恢复失败，原数据未改变") }
            finally { mutableUi.value = mutableUi.value.copy(busy = false) }
        }
    }
    fun selectRange(range: dev.goelo.android.stats.HistoryRange) { mutableUi.value = mutableUi.value.copy(range = range) }

    fun openRecord() {
        val rank = mutableUi.value.snapshot?.state?.profile?.lastOpponentRank ?: 7
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(recordOpen = true, rank = rank, recordText = "", opponentId = null, error = null)
    }

    fun closeRecord() {
        if (!mutableUi.value.busy) mutableUi.value = mutableUi.value.copy(recordOpen = false, error = null)
    }

    fun setRank(rank: Int) {
        if (mutableUi.value.busy) return
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(rank = rank.coerceIn(1, 9), error = null)
    }

    fun setRecordText(text: String) {
        if (mutableUi.value.busy) return
        pendingRequest = null
        mutableUi.value = mutableUi.value.copy(recordText = text, error = null)
    }

    fun submit(outcome: Outcome) {
        val state = mutableUi.value
        if (state.busy) return
        val ledger = state.snapshot?.state ?: return
        val playerId = ledger.profile?.id ?: return
        val opponentId = if (state.knownOpponentMode) state.opponentId else null
        if (state.knownOpponentMode && (opponentId == null || opponentId == playerId || ledger.allProfiles.none { it.id == opponentId })) {
            mutableUi.value = state.copy(error = "请选择一名已有棋手作为对手")
            return
        }
        val input = if (state.knownOpponentMode) null else parseRecord(state.rank, state.recordText).getOrElse {
            mutableUi.value = state.copy(error = "请输入不超过 20 盘的战绩，例如 11-8")
            return
        }
        val request = pendingRequest?.takeIf { it.input == input && it.outcome == outcome && it.playerId == playerId && it.opponentId == opponentId }
            ?: PendingRequest(newId(), input, outcome, clock.instant().toEpochMilli(), clock.zone.id, playerId, opponentId).also { pendingRequest = it }
        mutate(onSuccess = {
            pendingRequest = null
            mutableUi.value = mutableUi.value.copy(recordOpen = false, recordText = "", undo = it, error = null)
        }) {
            if (request.opponentId != null)
                matches.recordKnown(request.id, request.opponentId, request.outcome, request.atEpochMs, request.zoneId, request.playerId).undo
            else matches.record(request.id, requireNotNull(request.input), request.outcome, request.atEpochMs, request.zoneId, request.playerId).undo
        }
    }

    fun undoLastRecord() {
        val token = mutableUi.value.undo ?: return
        mutate(onSuccess = { mutableUi.value = mutableUi.value.copy(undo = null) }) { matches.undo(token); null }
    }

    fun editMatch(id: String, input: RecordInput, outcome: Outcome, revision: Long) = mutate {
        matches.edit(id, input, outcome, revision)
        null
    }

    fun deleteMatch(id: String, revision: Long) = mutate {
        matches.delete(id, revision)
        null
    }
    fun editKnownMatch(id: String, outcome: Outcome, revision: Long) = mutate {
        matches.editKnown(id, outcome, revision)
        null
    }

    fun renameProfile(name: String, revision: Long) = mutate {
        profiles.rename(name, revision)
        null
    }

    fun changeInitialElo(value: Double, revision: Long) = mutate {
        profiles.changeInitialElo(value, revision)
        null
    }

    fun setTarget(value: Double?, revision: Long) = mutate {
        profiles.setTarget(value, revision)
        null
    }

    private fun mutate(onSuccess: (UndoToken?) -> Unit = {}, block: suspend () -> UndoToken?) {
        if (mutableUi.value.busy) return
        mutableUi.value = mutableUi.value.copy(busy = true, error = null, undo = null)
        viewModelScope.launch {
            try {
                onSuccess(block())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message
                mutableUi.value = mutableUi.value.copy(error = when {
                    message?.contains("数据已变化") == true -> "记录已更新，请重新打开"
                    message.isNullOrBlank() -> "保存失败，记录未改变"
                    else -> message
                })
            } finally {
                mutableUi.value = mutableUi.value.copy(busy = false)
            }
        }
    }

    private data class PendingRequest(
        val id: String,
        val input: dev.goelo.android.model.RecordInput?,
        val outcome: Outcome,
        val atEpochMs: Long,
        val zoneId: String,
        val playerId: String,
        val opponentId: String?,
    )
}
