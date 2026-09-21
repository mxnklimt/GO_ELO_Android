package dev.goelo.android.stats

import dev.goelo.android.model.AppState
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.forPlayer
import dev.goelo.android.rating.scoreChange
import dev.goelo.android.rating.winProbability

data class PredictionResult(
    val opponentElo: Double,
    val winProbability: Double,
    val winDelta: Double,
    val lossDelta: Double,
)

fun predictAgainst(selfElo: Double, opponentElo: Double): PredictionResult {
    val probability = winProbability(selfElo, opponentElo)
    return PredictionResult(
        opponentElo = opponentElo,
        winProbability = probability,
        winDelta = scoreChange(selfElo, opponentElo, Outcome.WIN).delta,
        lossDelta = scoreChange(selfElo, opponentElo, Outcome.LOSS).delta,
    )
}

data class HeadToHeadMatch(
    val matchId: String,
    val order: Long,
    val outcome: Outcome,
    val playedAtEpochMs: Long?,
    val playedZoneId: String?,
)

data class HeadToHeadStat(
    val opponentId: String,
    val opponentName: String,
    val wins: Int,
    val losses: Int,
    val matches: List<HeadToHeadMatch>,
) {
    val games: Int get() = matches.size
    val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}

fun headToHead(state: AppState, currentPlayerId: String): List<HeadToHeadStat> {
    val names = state.allProfiles.associate { it.id to it.name }
    return state.forPlayer(currentPlayerId).matches
        .asSequence()
        .filter { it.kind == MatchKind.NATIVE && it.opponentPlayerId != null }
        .groupBy { requireNotNull(it.opponentPlayerId) }
        .map { (opponentId, matches) ->
            val rows = matches.sortedBy { it.order }.map { match ->
                HeadToHeadMatch(match.id, match.order, match.outcome, match.playedAtEpochMs, match.playedZoneId)
            }
            HeadToHeadStat(
                opponentId = opponentId,
                opponentName = names[opponentId] ?: "未知棋手",
                wins = rows.count { it.outcome == Outcome.WIN },
                losses = rows.count { it.outcome == Outcome.LOSS },
                matches = rows,
            )
        }
        .sortedWith(compareBy<HeadToHeadStat> { it.opponentName }.thenBy { it.opponentId })
        .toList()
}
