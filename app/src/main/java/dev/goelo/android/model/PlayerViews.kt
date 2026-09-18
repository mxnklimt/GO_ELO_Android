package dev.goelo.android.model

/** Read-only projection. Never persist this view: the shared ledger is authoritative. */
fun AppState.forPlayer(id: String): AppState {
    val player = requireNotNull(allProfiles.find { it.id == id }) { "未找到棋手" }
    val names = allProfiles.associate { it.id to it.name }
    val personal = matches.sortedBy { it.order }.mapNotNull { match ->
        when (id) {
            match.playerId -> match.copy(opponentName = match.opponentPlayerId?.let(names::get))
            match.opponentPlayerId -> match.copy(
                playerId = id, opponentPlayerId = match.playerId,
                outcome = match.outcome.opposite(),
                eloBefore = requireNotNull(match.opponentElo),
                delta = -match.delta, eloAfter = requireNotNull(match.opponentEloAfter),
                opponentElo = match.eloBefore, opponentEloAfter = match.eloAfter,
                opponentName = names[match.playerId],
            )
            else -> null
        }
    }
    return AppState(player, personal)
}

fun AppState.ratingOf(id: String): Double {
    val player = requireNotNull(allProfiles.find { it.id == id }) { "未找到棋手" }
    val last = matches.filter { it.playerId == id || it.opponentPlayerId == id }.maxByOrNull { it.order }
    return when {
        last == null -> player.initialElo
        last.playerId == id -> last.eloAfter
        else -> requireNotNull(last.opponentEloAfter)
    }
}
