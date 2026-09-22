package dev.goelo.android.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.goelo.android.model.LegacyOrigin
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
import dev.goelo.android.model.RecordInput
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = "local",
    val name: String,
    val initialElo: Double,
    val targetElo: Double?,
    val targetStartElo: Double?,
    val lastOpponentRank: Int,
)

@Entity(tableName = "matches", indices = [Index(value = ["orderIndex"], unique = true)])
data class MatchEntity(
    @PrimaryKey val id: String,
    val orderIndex: Long,
    val kind: String,
    val playedAtEpochMs: Long?,
    val playedZoneId: String?,
    val outcome: String,
    val opponentRank: Int?,
    val recordWins: Int?,
    val recordLosses: Int?,
    val opponentElo: Double?,
    val eloBefore: Double,
    val delta: Double,
    val eloAfter: Double,
    val ruleVersion: String,
    val legacyJson: String?,
    @ColumnInfo(defaultValue = "'local'") val playerId: String = "local",
    val opponentPlayerId: String? = null,
    val opponentEloAfter: Double? = null,
)

@Entity(tableName = "meta")
data class MetaEntity(@PrimaryKey val id: Int = 0, val revision: Long,
    @ColumnInfo(defaultValue = "'local'") val activePlayerId: String = "local")

@Entity(tableName = "dog_ids")
data class DogIdEntity(@PrimaryKey val id: String)

private val entityJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }

fun ProfileEntity.toModel() = Profile(id, name, initialElo, targetElo, targetStartElo, lastOpponentRank)
fun Profile.toEntity() = ProfileEntity(id, name, initialElo, targetElo, targetStartElo, lastOpponentRank)

fun MatchEntity.toModel(): Match = Match(
    id = id,
    order = orderIndex,
    kind = MatchKind.valueOf(kind),
    playedAtEpochMs = playedAtEpochMs,
    playedZoneId = playedZoneId,
    outcome = Outcome.valueOf(outcome),
    input = if (opponentRank == null) null else RecordInput(opponentRank, requireNotNull(recordWins), requireNotNull(recordLosses)),
    opponentElo = opponentElo,
    eloBefore = eloBefore,
    delta = delta,
    eloAfter = eloAfter,
    ruleVersion = ruleVersion,
    legacy = legacyJson?.let { entityJson.decodeFromString<LegacyOrigin>(it) },
    playerId = playerId, opponentPlayerId = opponentPlayerId, opponentEloAfter = opponentEloAfter,
)

fun Match.toEntity() = MatchEntity(
    id, order, kind.name, playedAtEpochMs, playedZoneId, outcome.name,
    input?.rank, input?.wins, input?.losses, opponentElo, eloBefore, delta, eloAfter,
    ruleVersion, legacy?.let { entityJson.encodeToString(it) },
    playerId, opponentPlayerId, opponentEloAfter,
)
