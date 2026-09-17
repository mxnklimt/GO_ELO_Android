package dev.goelo.android.rating

import dev.goelo.android.model.*
import java.time.ZoneId

fun replay(initialElo: Double, matches: List<Match>): List<Match> {
    require(initialElo.isFinite() && initialElo > 0)
    var elo = initialElo
    return matches.sortedWith(compareBy<Match>({ if (it.kind == MatchKind.LEGACY) 0 else 1 }, { it.order })).map { m ->
        val delta = if (m.kind == MatchKind.LEGACY) {
            val l = requireNotNull(m.legacy)
            l.sourceDelta * if (l.selfSide == 1) 1 else -1
        } else scoreChange(elo, opponentElo(requireNotNull(m.input)), m.outcome).delta
        val result = m.copy(eloBefore = elo, delta = delta, eloAfter = elo + delta)
        elo += delta
        result
    }
}

fun validateState(state: AppState): Result<Unit> = runCatching {
    val p = state.profile
    require(p != null || state.matches.isEmpty()) { "无档案时历史必须为空" }
    if (p != null) {
        require(p.id.isNotBlank() && p.name.isNotBlank() && p.initialElo.isFinite() && p.initialElo > 0)
        require(p.lastOpponentRank in 1..9)
        require((p.targetElo == null) == (p.targetStartElo == null))
        if (p.targetElo != null) require(p.targetElo.isFinite() && p.targetStartElo!!.isFinite() && p.targetElo > p.targetStartElo)
    }
    require(state.matches.map { it.id }.distinct().size == state.matches.size) { "重复 ID" }
    require(state.matches.map { it.order }.distinct().size == state.matches.size && state.matches.all { it.order > 0 }) { "order 无效" }
    state.matches.forEach { m ->
        require(m.id.isNotBlank() && m.eloBefore.isFinite() && m.delta.isFinite() && m.eloAfter.isFinite())
        when (m.kind) {
            MatchKind.NATIVE -> { require(m.input != null && m.opponentElo != null && m.playedAtEpochMs != null && m.playedZoneId != null); ZoneId.of(m.playedZoneId); require(m.legacy == null); require(m.ruleVersion == "elo-v1"); opponentElo(m.input) }
            MatchKind.LEGACY -> { val l = m.legacy ?: error("缺少 legacy 来源"); require(m.input == null && m.opponentElo == null && m.playedAtEpochMs == null && m.playedZoneId == null); require(m.ruleVersion == "legacy-fixed-v1"); require(l.lineNumber > 0 && l.selfSide in 1..2 && l.sourceResult in 0..1 && l.sourceDelta.isFinite() && Regex("[0-9a-fA-F]{64}").matches(l.fileSha256) && l.player1.isNotBlank() && l.player2.isNotBlank()); val expectedOutcome = if (l.sourceResult == 1) Outcome.WIN else Outcome.LOSS; require(m.outcome == if (l.selfSide == 1) expectedOutcome else if (expectedOutcome == Outcome.WIN) Outcome.LOSS else Outcome.WIN); require(kotlin.math.abs(m.delta - l.sourceDelta * if (l.selfSide == 1) 1 else -1) <= 1e-7) }
        }
    }
    if (p != null) { val canonical = state.matches.sortedWith(compareBy<Match>({ if (it.kind == MatchKind.LEGACY) 0 else 1 }, { it.order })); replay(p.initialElo, canonical).zip(canonical).forEach { (a, b) -> require(kotlin.math.abs(a.eloBefore-b.eloBefore) <= 1e-7 && kotlin.math.abs(a.eloAfter-b.eloAfter) <= 1e-7 && kotlin.math.abs(a.delta-b.delta) <= 1e-7) { "分值不一致" } } }
}
