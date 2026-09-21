package dev.goelo.android.data

import dev.goelo.android.model.*
import dev.goelo.android.rating.TEMPORARY_ELO_RULE_V2
import dev.goelo.android.rating.opponentElo
import dev.goelo.android.rating.replayState

data class UndoToken(val matchId: String, val revision: Long, val previousRank: Int, val playerId: String = "local")
data class RecordReceipt(val match: Match, val undo: UndoToken?)

class MatchService(private val store: StateStore) {
    suspend fun record(id: String, input: RecordInput, outcome: Outcome, at: Long, zoneId: String,
        playerId: String? = null): RecordReceipt = save(id, input, null, outcome, at, zoneId, playerId)

    suspend fun recordKnown(id: String, opponentId: String, outcome: Outcome, at: Long, zoneId: String,
        playerId: String): RecordReceipt = save(id, null, opponentId, outcome, at, zoneId, playerId)

    private suspend fun save(id: String, input: RecordInput?, opponentId: String?, outcome: Outcome,
        at: Long, zoneId: String, playerId: String?): RecordReceipt {
        require(id.isNotBlank()) { "记录 ID 不能为空" }
        require(at >= 0) { "对局时间无效" }
        val before = store.read()
        val ownerId = playerId ?: requireNotNull(before.state.profile).id
        val owner = requireNotNull(before.state.allProfiles.find { it.id == ownerId }) { "未找到棋手" }
        if (opponentId != null) {
            require(opponentId != ownerId) { "不能与自己对局" }
            require(before.state.allProfiles.any { it.id == opponentId }) { "未找到对手" }
        } else opponentElo(requireNotNull(input))
        fun Match.sameRequest() = this.playerId == ownerId && opponentPlayerId == opponentId &&
            this.input == input && this.outcome == outcome && playedAtEpochMs == at && playedZoneId == zoneId && kind == MatchKind.NATIVE
        before.state.matches.firstOrNull { it.id == id }?.let {
            require(it.sameRequest()) { "同一记录 ID 的内容不一致" }
            return RecordReceipt(it, null)
        }
        return try {
            val saved = store.update(before.revision) { state ->
                val seed = Match(id, (state.matches.maxOfOrNull { it.order } ?: 0) + 1,
                    MatchKind.NATIVE, at, zoneId, outcome, input, null, 0.0, 0.0, 0.0,
                    if (opponentId == null) TEMPORARY_ELO_RULE_V2 else "elo-pair-v1",
                    playerId = ownerId, opponentPlayerId = opponentId)
                val ranked = if (input == null) state else state.updatePlayer(ownerId) { it.copy(lastOpponentRank = input.rank) }
                rehydrated(ranked.copy(matches = state.matches + seed))
            }
            RecordReceipt(saved.state.matches.single { it.id == id }, UndoToken(id, saved.revision, owner.lastOpponentRank, ownerId))
        } catch (error: IllegalStateException) {
            val existing = store.read().state.matches.firstOrNull { it.id == id }
            if (existing != null && existing.sameRequest()) RecordReceipt(existing, null) else throw error
        }
    }

    suspend fun edit(id: String, input: RecordInput, outcome: Outcome, expectedRevision: Long): StoreSnapshot =
        store.update(expectedRevision) { state ->
            val old = editable(state, id)
            require(old.opponentPlayerId == null && old.playerId == state.profile?.id) { "请使用已有棋手对局更正" }
            rehydrated(state.copy(matches = state.matches.map {
                if (it.id == id) it.copy(input = input, outcome = outcome, ruleVersion = TEMPORARY_ELO_RULE_V2) else it
            }))
        }

    suspend fun editKnown(id: String, outcome: Outcome, expectedRevision: Long): StoreSnapshot =
        store.update(expectedRevision) { state ->
            val old = editable(state, id)
            require(old.opponentPlayerId != null) { "不是已有棋手对局" }
            val canonicalOutcome = if (state.profile?.id == old.playerId) outcome else outcome.opposite()
            rehydrated(state.copy(matches = state.matches.map { if (it.id == id) it.copy(outcome = canonicalOutcome) else it }))
        }

    suspend fun delete(id: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        editable(state, id)
        rehydrated(state.copy(matches = state.matches.filterNot { it.id == id }))
    }

    suspend fun undo(token: UndoToken): StoreSnapshot = store.update(token.revision) { state ->
        val last = state.matches.maxByOrNull { it.order }
        require(last?.id == token.matchId && last.kind == MatchKind.NATIVE && last.playerId == token.playerId) { "该记录已不能撤销" }
        rehydrated(state.updatePlayer(token.playerId) { it.copy(lastOpponentRank = token.previousRank) }
            .copy(matches = state.matches.filterNot { it.id == token.matchId }))
    }

    private fun editable(state: AppState, id: String): Match {
        val match = requireNotNull(state.matches.find { it.id == id }) { "未找到对局" }
        require(match.kind == MatchKind.NATIVE) { "旧历史不能编辑或删除" }
        require(state.profile?.id == match.playerId || state.profile?.id == match.opponentPlayerId) { "对局不属于当前棋手" }
        return match
    }
}

private fun AppState.updatePlayer(id: String, transform: (Profile) -> Profile): AppState = copy(
    profile = profile?.let { if (it.id == id) transform(it) else it },
    otherProfiles = otherProfiles.map { if (it.id == id) transform(it) else it },
)
internal fun currentElo(state: AppState): Double = state.ratingOf(requireNotNull(state.profile).id)
internal fun rehydrated(state: AppState): AppState = replayState(state)
