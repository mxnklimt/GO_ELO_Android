package dev.goelo.android.stats

import dev.goelo.android.model.*
import dev.goelo.android.rating.scoreChange
import dev.goelo.android.rating.winProbability
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisInsightsTest {
    private fun known(
        id: String,
        order: Long,
        playerId: String,
        opponentId: String,
        outcome: Outcome,
        at: Long,
    ) = Match(
        id, order, MatchKind.NATIVE, at, "UTC", outcome, null,
        2000.0, 2000.0, 10.0, 2010.0, "elo-pair-v1",
        playerId = playerId, opponentPlayerId = opponentId, opponentEloAfter = 1990.0,
    )

    @Test fun predictionUsesExpectedWinRateAndK20Deltas() {
        val result = predictAgainst(2000.0, 2100.0)
        assertEquals(2100.0, result.opponentElo, 0.0)
        assertEquals(winProbability(2000.0, 2100.0), result.winProbability, 1e-12)
        assertEquals(scoreChange(2000.0, 2100.0, Outcome.WIN).delta, result.winDelta, 1e-12)
        assertEquals(scoreChange(2000.0, 2100.0, Outcome.LOSS).delta, result.lossDelta, 1e-12)
    }

    @Test fun headToHeadGroupsKnownGamesAndReversesSecondPlayerPerspective() {
        val state = AppState(
            Profile(id = "local", name = "甲", initialElo = 2000.0),
            listOf(
                known("ab-win", 1, "local", "b", Outcome.WIN, 1),
                known("ba-win", 2, "b", "local", Outcome.WIN, 2),
                known("ac-loss", 3, "local", "c", Outcome.LOSS, 3),
                Match("temporary", 4, MatchKind.NATIVE, 4, "UTC", Outcome.WIN, RecordInput(7, 1, 1), 2300.0, 2000.0, 5.0, 2005.0, "elo-v3"),
            ),
            otherProfiles = listOf(
                Profile(id = "b", name = "Bob", initialElo = 2000.0),
                Profile(id = "c", name = "Carol", initialElo = 2100.0),
            ),
        )

        val result = headToHead(state, "local")
        assertEquals(listOf("b", "c"), result.map { it.opponentId })
        assertEquals(listOf("Bob", "Carol"), result.map { it.opponentName })
        assertEquals(1, result[0].wins)
        assertEquals(1, result[0].losses)
        assertEquals(2, result[0].matches.size)
        assertEquals(listOf(Outcome.WIN, Outcome.LOSS), result[0].matches.map { it.outcome })
        assertEquals(0, result[1].wins)
        assertEquals(1, result[1].losses)
        assertEquals(1, result[1].matches.size)
    }
}
