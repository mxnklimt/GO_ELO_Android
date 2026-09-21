package dev.goelo.android.rating

import dev.goelo.android.model.*
import java.time.ZoneId
import kotlin.math.abs

/** Legacy single-player API retained for existing consumers and tests. */
fun replay(initialElo: Double, matches: List<Match>): List<Match> =
    replayState(AppState(Profile(name = "棋手", initialElo = initialElo), matches)).matches

fun replayState(state: AppState): AppState {
    val ratings = state.allProfiles.associate { it.id to it.initialElo }.toMutableMap()
    val replayed = state.matches.sortedBy { it.order }.map { m ->
        val before = requireNotNull(ratings[m.playerId]) { "对局棋手不存在" }
        val otherId = m.opponentPlayerId
        val opponent = if (otherId != null) {
            require(otherId != m.playerId) { "不能与自己对局" }
            requireNotNull(ratings[otherId]) { "对手棋手不存在" }
        } else m.input?.let { opponentElo(it, m.ruleVersion) }
        val delta = if (m.kind == MatchKind.LEGACY) {
            val l = requireNotNull(m.legacy)
            l.sourceDelta * if (l.selfSide == 1) 1 else -1
        } else scoreChange(before, requireNotNull(opponent), m.outcome).delta
        ratings[m.playerId] = before + delta
        val otherAfter = if (otherId != null) (requireNotNull(opponent) - delta).also { ratings[otherId] = it } else null
        m.copy(eloBefore = before, delta = delta, eloAfter = before + delta,
            opponentElo = opponent, opponentEloAfter = otherAfter, opponentName = null)
    }
    return state.copy(matches = replayed)
}

fun validateState(state: AppState): Result<Unit> = runCatching {
    require(state.profile != null || (state.matches.isEmpty() && state.otherProfiles.isEmpty())) { "无档案时历史必须为空" }
    val players = state.allProfiles
    require(players.map { it.id }.distinct().size == players.size) { "重复棋手 ID" }
    players.forEach { p ->
        require(p.id.isNotBlank() && p.name.isNotBlank() && p.initialElo.isFinite() && p.initialElo > 0) { "棋手档案无效" }
        require(p.lastOpponentRank in 1..9)
        require((p.targetElo == null) == (p.targetStartElo == null))
        if (p.targetElo != null) require(p.targetElo.isFinite() && p.targetStartElo!!.isFinite() && p.targetElo > p.targetStartElo)
    }
    val ids = players.map { it.id }.toSet()
    require(state.matches.map { it.id }.distinct().size == state.matches.size) { "重复 ID" }
    require(state.matches.map { it.order }.distinct().size == state.matches.size && state.matches.all { it.order > 0 }) { "order 无效" }
    val firstNative = state.matches.filter { it.kind == MatchKind.NATIVE }.minOfOrNull { it.order }
    val lastLegacy = state.matches.filter { it.kind == MatchKind.LEGACY }.maxOfOrNull { it.order }
    require(firstNative == null || lastLegacy == null || lastLegacy < firstNative) { "旧历史不能排在新记录之后" }
    state.matches.forEachIndexed { index, m ->
        val path = "matches[$index]"
        require(m.playerId in ids) { "$path.playerId 不存在" }
        require(m.id.isNotBlank() && m.eloBefore.isFinite() && m.delta.isFinite() && m.eloAfter.isFinite()) { "$path 分数无效" }
        require(m.opponentName == null) { "不能保存视图副本" }
        if (m.opponentPlayerId != null) {
            require(m.opponentPlayerId in ids && m.opponentPlayerId != m.playerId) { "$path 对手不存在或与自己对局" }
            require(m.opponentEloAfter?.isFinite() == true)
        } else require(m.opponentEloAfter == null)
        when (m.kind) {
            MatchKind.NATIVE -> {
                require(m.opponentElo?.isFinite() == true && m.playedAtEpochMs != null && m.playedAtEpochMs >= 0 && m.playedZoneId != null)
                ZoneId.of(m.playedZoneId)
                require(m.legacy == null)
                if (m.opponentPlayerId == null) {
                    require(m.ruleVersion == TEMPORARY_ELO_RULE_V1 || m.ruleVersion == TEMPORARY_ELO_RULE_V2 || m.ruleVersion == TEMPORARY_ELO_RULE_V3) { "$path.ruleVersion 无效" }
                    require(m.input != null)
                    require(abs(requireNotNull(m.opponentElo) - opponentElo(m.input, m.ruleVersion)) <= 1e-7)
                } else {
                    require(m.ruleVersion == "elo-pair-v1") { "$path.ruleVersion 无效" }
                    require(m.input == null) { "已有棋手不使用估分战绩" }
                }
            }
            MatchKind.LEGACY -> {
                val l = m.legacy ?: error("缺少 legacy 来源")
                require(m.input == null && m.opponentElo == null && m.opponentPlayerId == null && m.playedAtEpochMs == null && m.playedZoneId == null)
                require(m.ruleVersion == "legacy-fixed-v1") { "$path.ruleVersion 无效" }
                require(l.lineNumber > 0 && l.selfSide in 1..2 && l.sourceResult in 0..1 && l.sourceDelta.isFinite() &&
                    Regex("[0-9a-fA-F]{64}").matches(l.fileSha256) && l.player1.isNotBlank() && l.player2.isNotBlank())
                val firstOutcome = if (l.sourceResult == 1) Outcome.WIN else Outcome.LOSS
                require(m.outcome == if (l.selfSide == 1) firstOutcome else firstOutcome.opposite())
                require(abs(m.delta - l.sourceDelta * if (l.selfSide == 1) 1 else -1) <= 1e-7)
            }
        }
    }
    val canonical = state.matches.sortedBy { it.order }
    replayState(state).matches.zip(canonical).forEach { (a, b) ->
        require(abs(a.eloBefore-b.eloBefore) <= 1e-7 && abs(a.eloAfter-b.eloAfter) <= 1e-7 && abs(a.delta-b.delta) <= 1e-7) { "分值不一致" }
        if (a.opponentPlayerId != null) require(
            abs(requireNotNull(a.opponentElo)-requireNotNull(b.opponentElo)) <= 1e-7 &&
                abs(requireNotNull(a.opponentEloAfter)-requireNotNull(b.opponentEloAfter)) <= 1e-7
        ) { "对手分值不一致" }
    }
}
