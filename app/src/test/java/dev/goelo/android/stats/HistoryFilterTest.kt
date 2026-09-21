package dev.goelo.android.stats

import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HistoryFilterTest {
    private fun native(id: String, order: Long, rank: Int, date: String? = "2026-09-17", zone: String = "UTC", opponentId: String? = null) = Match(
        id, order, MatchKind.NATIVE,
        date?.let { java.time.LocalDate.parse(it).atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli() },
        date?.let { zone }, Outcome.WIN, if (opponentId == null) RecordInput(rank, 0, 0) else null, 2000.0, 2000.0, 0.0, 2000.0,
        if (opponentId == null) "elo-v1" else "elo-pair-v1", playerId = "local", opponentPlayerId = opponentId,
    )

    private fun legacy(id: String, order: Long) = Match(
        id, order, MatchKind.LEGACY, null, null, Outcome.LOSS, null, null,
        2000.0, 0.0, 2000.0, "legacy-fixed-v1",
    )

    @Test fun dateBoundsUseEachMatchLocalDateAndNewestOrderFirst() {
        val shanghaiMidnight = Instant.parse("2026-09-16T16:00:00Z").toEpochMilli()
        val rows = listOf(
            native("before", 1, 7, "2026-09-16"),
            native("inside", 2, 7, "2026-09-17", "Asia/Shanghai"),
            native("after", 3, 7, "2026-09-18"),
        ).toMutableList().also { it[1] = it[1].copy(playedAtEpochMs = shanghaiMidnight) }

        assertEquals(listOf("inside"), filterMatches(rows, HistoryFilter(fromDate = "2026-09-17", toDate = "2026-09-17")).map { it.id })
    }

    @Test fun undatedOnlyIgnoresDateBoundsAndKnownRankFilteringDoesNotMutateSource() {
        val rows = listOf(native("rank-seven", 1, 7), legacy("old", 2), native("rank-six", 3, 6))
        assertEquals(listOf("old"), filterMatches(rows, HistoryFilter(rank = 7, fromDate = "2026-09-17", toDate = "2026-09-17", undatedOnly = true)).map { it.id })
        assertEquals(3, rows.size)
        assertEquals(listOf("rank-seven"), filterMatches(rows, HistoryFilter(rank = 7)).map { it.id })
    }

    @Test fun invalidDateRangeIsRejected() {
        assertTrue(runCatching { filterMatches(emptyList(), HistoryFilter(fromDate = "2026-09-18", toDate = "2026-09-17")) }.isFailure)
        assertTrue(runCatching { filterMatches(emptyList(), HistoryFilter(fromDate = "not-a-date")) }.isFailure)
    }

    @Test fun matchScopeSeparatesNativeTemporaryAndPlayerLibraryGames() {
        val rows = listOf(native("temp", 1, 7), native("known", 2, 7, opponentId = "b"), legacy("old", 3))
        assertEquals(listOf("known"), filterMatches(rows, HistoryFilter(scope = MatchScope.PLAYER_LIBRARY)).map { it.id })
        assertEquals(listOf("temp"), filterMatches(rows, HistoryFilter(scope = MatchScope.TEMPORARY)).map { it.id })
        assertEquals(listOf("old", "known", "temp"), filterMatches(rows, HistoryFilter()).map { it.id })
    }

    @Test fun matchScopeComposesWithOutcomeAndRank() {
        val rows = listOf(
            native("known-win", 1, 7, opponentId = "b"),
            native("known-other-rank", 2, 6, opponentId = "b").copy(outcome = Outcome.LOSS),
        )
        assertEquals(listOf("known-win"), filterMatches(rows, HistoryFilter(outcome = Outcome.WIN, scope = MatchScope.PLAYER_LIBRARY)).map { it.id })
    }
}
