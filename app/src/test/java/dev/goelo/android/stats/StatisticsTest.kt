package dev.goelo.android.stats

import dev.goelo.android.model.*
import org.junit.Assert.*
import org.junit.Test

class StatisticsTest {
    private fun state(vararg outcomes: Outcome): AppState {
        val p = Profile(name = "棋手", initialElo = 2000.0)
        val ms = outcomes.mapIndexed { i, o -> Match("m$i", (i + 1).toLong(), MatchKind.NATIVE, 1000L + i, "UTC", o, RecordInput(6, 0, 0), 2000.0, 2000.0, 0.0, 2000.0, "elo-v1") }
        return AppState(p, ms)
    }
    @Test fun emptyAndStreaks() {
        assertNull(summarize(AppState(Profile(name="x", initialElo=2000.0), emptyList()), HistoryRange.ALL).winRate)
        val s = summarize(state(Outcome.WIN, Outcome.WIN, Outcome.LOSS, Outcome.WIN), HistoryRange.ALL)
        assertEquals(4, s.games); assertEquals(0.75, s.winRate!!, 0.0); assertEquals(1, s.currentStreak); assertEquals(2, s.longestStreak)
    }
    @Test fun rangeUsesNewestGamesAndSeriesHasStart() {
        val s = state(*Array(21) { if (it == 0) Outcome.LOSS else Outcome.WIN })
        assertEquals(20, summarize(s, HistoryRange.LAST20).games)
        assertEquals(21, series(s, HistoryRange.ALL).size); assertNull(series(s, HistoryRange.ALL).first().matchId)
    }
    @Test fun unknownMonthIsExcludedAndRankGroupsNativeOnly() {
        val ms = state(Outcome.WIN).matches + Match("old", 2, MatchKind.LEGACY, 1000, "UTC", Outcome.LOSS, null, null, 2000.0, 0.0, 2000.0, "legacy")
        assertEquals(1, byMonth(ms).single().games); assertEquals(1, byRank(ms).single().wins)
    }
}
