package dev.goelo.android.data

import dev.goelo.android.model.AppState
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.replay

data class UndoToken(val matchId: String, val revision: Long, val previousRank: Int)
data class RecordReceipt(val match: Match, val undo: UndoToken?)

class MatchService(private val store: StateStore) {
    suspend fun record(id: String, input: RecordInput, outcome: Outcome, at: Long, zoneId: String): RecordReceipt {
        require(id.isNotBlank()) { "记录 ID 不能为空" }
        require(at >= 0) { "对局时间无效" }
        val before = store.read()
        before.state.matches.firstOrNull { it.id == id }?.let { existing ->
            require(existing.sameRequest(input, outcome, at, zoneId)) { "同一记录 ID 的内容不一致" }
            return RecordReceipt(existing, null)
        }
        val profile = requireNotNull(before.state.profile) { "请先创建档案" }
        val nextState = fun(state: AppState): AppState {
            val existing = state.matches.firstOrNull { it.id == id }
            if (existing != null) {
                require(existing.sameRequest(input, outcome, at, zoneId)) { "同一记录 ID 的内容不一致" }
                state
            } else {
                val currentProfile = requireNotNull(state.profile) { "请先创建档案" }
                val nextOrder = (state.matches.maxOfOrNull { it.order } ?: 0L) + 1L
                val beforeElo = currentElo(state)
                val seed = Match(
                    id, nextOrder, MatchKind.NATIVE, at, zoneId, outcome, input, opponentElo(input),
                    beforeElo, 0.0, beforeElo, "elo-v1",
                )
                rehydrated(state.copy(profile = currentProfile.copy(lastOpponentRank = input.rank), matches = state.matches + seed))
            }
        }
        return try {
            val saved = store.update(before.revision, nextState)
            val match = requireNotNull(saved.state.matches.firstOrNull { it.id == id })
            val undo = if (saved.state.matches.size == before.state.matches.size) null else
                UndoToken(id, saved.revision, profile.lastOpponentRank)
            RecordReceipt(match, undo)
        } catch (error: IllegalStateException) {
            // A concurrent retry may already have committed this request. It is the only retry allowed.
            val after = store.read()
            val existing = after.state.matches.firstOrNull { it.id == id }
            if (existing != null && existing.sameRequest(input, outcome, at, zoneId)) RecordReceipt(existing, null) else throw error
        }
    }

    suspend fun edit(id: String, input: RecordInput, outcome: Outcome, expectedRevision: Long): StoreSnapshot =
        store.update(expectedRevision) { state ->
            val old = requireNotNull(state.matches.firstOrNull { it.id == id }) { "未找到对局" }
            require(old.kind == MatchKind.NATIVE) { "旧历史不能编辑" }
            rehydrated(state.copy(matches = state.matches.map { match ->
                if (match.id == id) match.copy(input = input, opponentElo = opponentElo(input), outcome = outcome) else match
            }))
        }

    suspend fun delete(id: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val old = requireNotNull(state.matches.firstOrNull { it.id == id }) { "未找到对局" }
        require(old.kind == MatchKind.NATIVE) { "旧历史不能删除" }
        rehydrated(state.copy(matches = state.matches.filterNot { it.id == id }))
    }

    suspend fun undo(token: UndoToken): StoreSnapshot = store.update(token.revision) { state ->
        val last = state.matches.maxByOrNull { it.order }
        require(last?.id == token.matchId && last.kind == MatchKind.NATIVE) { "该记录已不能撤销" }
        val profile = requireNotNull(state.profile)
        rehydrated(state.copy(profile = profile.copy(lastOpponentRank = token.previousRank), matches = state.matches.filterNot { it.id == token.matchId }))
    }

    private fun Match.sameRequest(input: RecordInput, outcome: Outcome, at: Long, zoneId: String) =
        kind == MatchKind.NATIVE && this.input == input && this.outcome == outcome && playedAtEpochMs == at && playedZoneId == zoneId
}

internal fun currentElo(state: AppState): Double =
    state.matches.maxByOrNull { it.order }?.eloAfter ?: requireNotNull(state.profile).initialElo

/** Replays in chronological semantic order but retains persisted order positions. */
internal fun rehydrated(state: AppState): AppState {
    val profile = requireNotNull(state.profile)
    val recalculated = replay(profile.initialElo, state.matches).associateBy { it.id }
    return state.copy(matches = state.matches.map { requireNotNull(recalculated[it.id]) })
}
