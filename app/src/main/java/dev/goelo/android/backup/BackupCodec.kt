package dev.goelo.android.backup

import dev.goelo.android.model.*
import dev.goelo.android.rating.validateState
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class BackupEnvelope(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAtEpochMs: Long,
    val appVersion: String,
    val state: AppState,
) {
    companion object {
        const val FORMAT = "go-elo-backup"
        const val SCHEMA_VERSION = 2
    }
}

/** Strict v1 reader and v2 writer. Decode normalizes v1 before it reaches restore/snapshots. */
class BackupCodec {
    fun encode(envelope: BackupEnvelope): ByteArray {
        validateEnvelope(envelope)
        return json.encodeToString(envelope).encodeToByteArray().also {
            require(it.size <= MAX_ARCHIVE_BYTES) { "备份文件超过 32 MiB 上限" }
        }
    }

    fun decode(bytes: ByteArray): BackupEnvelope {
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "备份文件超过 32 MiB 上限" }
        val root = requireObject(json.parseToJsonElement(strictUtf8(bytes)), "备份")
        requireKeys(root, ENVELOPE_KEYS, "备份")
        val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
        require(version == 1 || version == 2) { "不支持的备份版本" }
        validateShape(root, version)
        val envelope = json.decodeFromJsonElement<BackupEnvelope>(root)
        if (version == 1) require(envelope.state.profile == null || envelope.state.profile.id == "local") { "旧档案 ID 无效" }
        val upgraded = envelope.copy(schemaVersion = BackupEnvelope.SCHEMA_VERSION)
        validateEnvelope(upgraded)
        return upgraded
    }

    private fun validateEnvelope(envelope: BackupEnvelope) {
        require(envelope.format == BackupEnvelope.FORMAT) { "不支持的备份格式" }
        require(envelope.schemaVersion == BackupEnvelope.SCHEMA_VERSION) { "不支持的备份版本" }
        require(envelope.exportedAtEpochMs >= 0) { "导出时间无效" }
        require(envelope.appVersion.isNotBlank()) { "应用版本不能为空" }
        require(envelope.state.matches.size <= MAX_MATCHES) { "对局数量超过上限" }
        require(envelope.state.allProfiles.size <= MAX_PLAYERS) { "棋手数量超过上限" }
        val seen = mutableSetOf<String>()
        envelope.state.matches.forEachIndexed { index, match ->
            require(seen.add(match.id)) { "state.matches[$index].id 重复" }
        }
        validateState(envelope.state).getOrElse { throw IllegalArgumentException("state 无效：${it.message}", it) }
    }

    private fun validateShape(root: JsonObject, version: Int) {
        val state = requireObject(root.getValue("state"), "state")
        requireKeys(state, if (version == 1) STATE_KEYS else STATE_KEYS + "otherProfiles", "state")
        val profile = state.getValue("profile")
        if (profile !is JsonNull) requireKeys(requireObject(profile, "profile"), PROFILE_KEYS, "profile")
        if (version == 2) {
            val others = state["otherProfiles"] as? JsonArray ?: error("otherProfiles 必须是数组")
            require(others.size < MAX_PLAYERS)
            others.forEachIndexed { index, p -> requireKeys(requireObject(p, "otherProfiles[$index]"), PROFILE_KEYS, "otherProfiles[$index]") }
        }
        val matches = state["matches"] as? JsonArray ?: error("state.matches 必须是数组")
        require(matches.size <= MAX_MATCHES)
        matches.forEachIndexed { index, element ->
            val match = requireObject(element, "matches[$index]")
            requireKeys(match, if (version == 1) MATCH_KEYS else MATCH_KEYS + PLAYER_MATCH_KEYS, "matches[$index]")
            val input = match.getValue("input")
            if (input !is JsonNull) requireKeys(requireObject(input, "matches[$index].input"), INPUT_KEYS, "matches[$index].input")
            val legacy = match.getValue("legacy")
            if (legacy !is JsonNull) requireKeys(requireObject(legacy, "matches[$index].legacy"), LEGACY_KEYS, "matches[$index].legacy")
        }
    }

    private fun requireObject(element: JsonElement, label: String): JsonObject =
        element as? JsonObject ?: throw IllegalArgumentException("$label 必须是对象")

    private fun requireKeys(value: JsonObject, expected: Set<String>, label: String) {
        require((expected - value.keys).isEmpty()) { "$label 缺少字段：${(expected - value.keys).sorted().joinToString()}" }
        require((value.keys - expected).isEmpty()) { "$label 包含未知字段：${(value.keys - expected).sorted().joinToString()}" }
    }

    private fun strictUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException("备份文件不是有效 UTF-8", error)
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024
        const val MAX_MATCHES = 100_000
        const val MAX_PLAYERS = 10_000
        val ENVELOPE_KEYS = setOf("format", "schemaVersion", "exportedAtEpochMs", "appVersion", "state")
        val STATE_KEYS = setOf("profile", "matches")
        val PROFILE_KEYS = setOf("id", "name", "initialElo", "targetElo", "targetStartElo", "lastOpponentRank")
        val MATCH_KEYS = setOf("id", "order", "kind", "playedAtEpochMs", "playedZoneId", "outcome", "input",
            "opponentElo", "eloBefore", "delta", "eloAfter", "ruleVersion", "legacy")
        val PLAYER_MATCH_KEYS = setOf("playerId", "opponentPlayerId", "opponentEloAfter")
        val INPUT_KEYS = setOf("rank", "wins", "losses")
        val LEGACY_KEYS = setOf("fileSha256", "lineNumber", "selfSide", "player1", "player2", "sourceResult", "sourceDelta", "charset")
        val json = Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
            isLenient = false
            allowSpecialFloatingPointValues = false
        }
    }
}
