package dev.goelo.android.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable enum class Outcome { WIN, LOSS }
@Serializable enum class MatchKind { NATIVE, LEGACY }
@Serializable data class RecordInput(val rank: Int, val wins: Int, val losses: Int)
@Serializable data class Profile(val id: String = "local", val name: String, val initialElo: Double, val targetElo: Double? = null, val targetStartElo: Double? = null, val lastOpponentRank: Int = 7)
@Serializable data class LegacyOrigin(val fileSha256: String, val lineNumber: Int, val selfSide: Int, val player1: String, val player2: String, val sourceResult: Int, val sourceDelta: Double, val charset: String)
@Serializable data class Match(
    val id: String, val order: Long, val kind: MatchKind, val playedAtEpochMs: Long?,
    val playedZoneId: String?, val outcome: Outcome, val input: RecordInput?,
    val opponentElo: Double?, val eloBefore: Double, val delta: Double, val eloAfter: Double,
    val ruleVersion: String, val legacy: LegacyOrigin? = null,
    val playerId: String = "local", val opponentPlayerId: String? = null,
    val opponentEloAfter: Double? = null,
    @Transient val opponentName: String? = null,
)
@Serializable data class AppState(
    val profile: Profile?, val matches: List<Match>, val otherProfiles: List<Profile> = emptyList(),
) {
    val allProfiles: List<Profile> get() = listOfNotNull(profile) + otherProfiles
}
data class ScoreChange(val before: Double, val delta: Double, val after: Double)
fun Outcome.opposite(): Outcome = if (this == Outcome.WIN) Outcome.LOSS else Outcome.WIN
