package dev.goelo.android.rating

import dev.goelo.android.model.*
import kotlin.math.log10
import kotlin.math.pow

const val TEMPORARY_ELO_RULE_V1 = "elo-v1"
const val TEMPORARY_ELO_RULE_V2 = "elo-v2"
const val TEMPORARY_ELO_RULE_V3 = "elo-v3"

/** Current reference table: 1d = 1100, each additional dan adds 200. */
fun rankBaseline(rank: Int): Double {
    require(rank in 1..9)
    return 1100.0 + 200.0 * (rank - 1)
}

/** Estimates a temporary opponent using the current correction rule. */
fun opponentElo(input: RecordInput): Double = opponentElo(input, TEMPORARY_ELO_RULE_V3)

/** Replays a temporary opponent estimate with the rule stored on the match. */
fun opponentElo(input: RecordInput, ruleVersion: String): Double {
    require(input.rank in 1..9)
    require(input.wins in 0..20 && input.losses in 0..20)
    require(input.wins + input.losses <= 20)
    val coefficient = when (ruleVersion) {
        TEMPORARY_ELO_RULE_V1 -> 400.0
        TEMPORARY_ELO_RULE_V2 -> 100.0
        TEMPORARY_ELO_RULE_V3 -> 100.0
        else -> error("未知的临时对手估分规则")
    }
    val base = if (ruleVersion == TEMPORARY_ELO_RULE_V3) rankBaseline(input.rank)
    else 1000.0 + 200.0 * (input.rank - 1)
    val correction = when {
        input.wins == 0 && input.losses == 0 -> 0.0
        input.losses == 0 -> 600.0
        input.wins == 0 -> -600.0
        else -> coefficient * log10(input.wins.toDouble() / input.losses)
    }
    return (base + correction).also { require(it.isFinite()) }
}

fun winProbability(selfElo: Double, opponentElo: Double): Double {
    require(selfElo.isFinite() && opponentElo.isFinite())
    return 1.0 / (1.0 + 10.0.pow((opponentElo - selfElo) / 400.0))
}

fun scoreChange(selfElo: Double, opponentElo: Double, outcome: Outcome): ScoreChange {
    require(selfElo.isFinite() && opponentElo.isFinite())
    val expected = winProbability(selfElo, opponentElo)
    val delta = 20.0 * ((if (outcome == Outcome.WIN) 1.0 else 0.0) - expected)
    return ScoreChange(selfElo, delta, selfElo + delta)
}
