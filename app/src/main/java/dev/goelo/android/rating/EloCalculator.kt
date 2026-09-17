package dev.goelo.android.rating

import dev.goelo.android.model.*
import kotlin.math.log10
import kotlin.math.pow

fun opponentElo(input: RecordInput): Double {
    require(input.rank in 1..9)
    require(input.wins in 0..20 && input.losses in 0..20)
    require(input.wins + input.losses <= 20)
    val base = 1000.0 + 200.0 * (input.rank - 1)
    val correction = when {
        input.wins == 0 && input.losses == 0 -> 0.0
        input.losses == 0 -> 600.0
        input.wins == 0 -> -600.0
        else -> 400.0 * log10(input.wins.toDouble() / input.losses)
    }
    return (base + correction).also { require(it.isFinite()) }
}

fun scoreChange(selfElo: Double, opponentElo: Double, outcome: Outcome): ScoreChange {
    require(selfElo.isFinite() && opponentElo.isFinite())
    val expected = 1.0 / (1.0 + 10.0.pow((opponentElo - selfElo) / 400.0))
    val delta = 20.0 * ((if (outcome == Outcome.WIN) 1.0 else 0.0) - expected)
    return ScoreChange(selfElo, delta, selfElo + delta)
}
