package dev.goelo.android.stats

import dev.goelo.android.model.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryRange(val count: Int?) { LAST20(20), LAST50(50), LAST100(100), ALL(null) }
data class SeriesPoint(
    val order: Long, val elo: Double, val matchId: String?,
    val outcome: Outcome? = null, val delta: Double? = null,
)
data class Summary(val games: Int, val wins: Int, val winRate: Double?, val netDelta: Double,
    val currentStreak: Int, val longestStreak: Int, val peak: Double)
data class RankStat(val rank: Int, val games: Int, val wins: Int)
data class MonthStat(val month: String, val games: Int, val wins: Int, val netDelta: Double)
data class GoalView(val rankLabel: String, val target: Double?, val remaining: Double?,
    val progress: Double?, val achieved: Boolean)

private fun AppState.selected(range: HistoryRange): List<Match> =
    if (range.count == null) matches else matches.takeLast(range.count)

fun summarize(state: AppState, range: HistoryRange): Summary {
    val selected = state.selected(range)
    val all = state.matches
    val wins = selected.count { it.outcome == Outcome.WIN }
    var current = 0
    for (m in all.asReversed()) { if (m.outcome == Outcome.WIN) current++ else break }
    var run = 0; var longest = 0
    all.forEach { if (it.outcome == Outcome.WIN) { run++; longest = maxOf(longest, run) } else run = 0 }
    val initial = state.profile?.initialElo ?: all.firstOrNull()?.eloBefore ?: 0.0
    val peak = (listOf(initial) + all.map { it.eloAfter }).maxOrNull() ?: initial
    return Summary(selected.size, wins, wins.toDouble().takeIf { selected.isNotEmpty() }?.div(selected.size),
        selected.sumOf { it.delta }, current, longest, peak)
}

fun series(state: AppState, range: HistoryRange): List<SeriesPoint> {
    val selected = state.selected(range)
    if (selected.isEmpty()) return if (state.profile == null) emptyList() else listOf(SeriesPoint(0, state.profile.initialElo, null))
    return listOf(SeriesPoint(selected.first().order - 1, selected.first().eloBefore, null)) +
        selected.map { SeriesPoint(it.order, it.eloAfter, it.id, it.outcome, it.delta) }
}

fun byRank(matches: List<Match>): List<RankStat> = matches.filter { it.kind == MatchKind.NATIVE && it.input != null }
    .groupBy { it.input!!.rank }.toSortedMap().map { (rank, ms) -> RankStat(rank, ms.size, ms.count { it.outcome == Outcome.WIN }) }

fun byMonth(matches: List<Match>): List<MonthStat> = matches.mapNotNull { m ->
    val time = m.playedAtEpochMs ?: return@mapNotNull null
    val zone = runCatching { ZoneId.of(m.playedZoneId ?: return@mapNotNull null) }.getOrNull() ?: return@mapNotNull null
    YearMonth.from(Instant.ofEpochMilli(time).atZone(zone)).toString() to m
}.groupBy({ it.first }, { it.second }).toSortedMap().map { (month, ms) ->
    MonthStat(month, ms.size, ms.count { it.outcome == Outcome.WIN }, ms.sumOf { it.delta })
}
