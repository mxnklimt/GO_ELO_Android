package dev.goelo.android.stats

import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class MatchScope { ALL, TEMPORARY, PLAYER_LIBRARY }

data class HistoryFilter(
    val rank: Int? = null,
    val outcome: Outcome? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val undatedOnly: Boolean = false,
    val scope: MatchScope = MatchScope.ALL,
)

/** Filters a copy of the visible history; storage order and complete history remain unchanged. */
fun filterMatches(matches: List<Match>, filter: HistoryFilter): List<Match> {
    require(filter.rank == null || filter.rank in 1..9) { "段位范围无效" }
    val from = filter.fromDate?.let(::parseDate)
    val to = filter.toDate?.let(::parseDate)
    require(from == null || to == null || from <= to) { "开始日期不能晚于结束日期" }
    return matches.asSequence()
        .filter { match ->
            if (!match.matchesScope(filter.scope)) return@filter false
            if (filter.undatedOnly) return@filter match.playedAtEpochMs == null || match.playedZoneId == null
            if (filter.rank != null && match.input?.rank != filter.rank) return@filter false
            if (filter.outcome != null && match.outcome != filter.outcome) return@filter false
            val date = match.localDateOrNull()
            (from == null || (date != null && date >= from)) && (to == null || (date != null && date <= to))
        }
        .sortedByDescending { it.order }
        .toList()
}

private fun Match.matchesScope(scope: MatchScope): Boolean = when (scope) {
    MatchScope.ALL -> true
    MatchScope.TEMPORARY -> kind == MatchKind.NATIVE && opponentPlayerId == null
    MatchScope.PLAYER_LIBRARY -> kind == MatchKind.NATIVE && opponentPlayerId != null
}

private fun parseDate(text: String): LocalDate = runCatching { LocalDate.parse(text) }
    .getOrElse { throw IllegalArgumentException("日期格式应为 YYYY-MM-DD") }

private fun Match.localDateOrNull(): LocalDate? {
    val time = playedAtEpochMs ?: return null
    val zone = playedZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: return null
    return Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
}
