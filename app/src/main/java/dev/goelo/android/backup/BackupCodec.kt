package dev.goelo.android.backup

import dev.goelo.android.model.AppState
import dev.goelo.android.rating.validateState
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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
        const val SCHEMA_VERSION = 1
    }
}

/** Encodes only complete, self-consistent archives and rejects permissive JSON input. */
class BackupCodec {
    fun encode(envelope: BackupEnvelope): ByteArray {
        validateEnvelope(envelope)
        return json.encodeToString(envelope).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): BackupEnvelope {
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "备份文件超过 32 MiB 上限" }
        val text = strictUtf8(bytes)
        val root = try {
            json.parseToJsonElement(text)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("备份 JSON 无法读取", error)
        }
        requireObject(root, "根对象")
        validateShape(root as JsonObject)
        val envelope = try {
            json.decodeFromJsonElement(BackupEnvelope.serializer(), root)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("备份字段格式无效", error)
        }
        validateEnvelope(envelope)
        return envelope
    }

    private fun validateEnvelope(envelope: BackupEnvelope) {
        require(envelope.format == BackupEnvelope.FORMAT) { "不支持的备份格式" }
        require(envelope.schemaVersion == BackupEnvelope.SCHEMA_VERSION) { "不支持的备份版本" }
        require(envelope.exportedAtEpochMs >= 0L) { "导出时间无效" }
        require(envelope.appVersion.isNotBlank()) { "应用版本不能为空" }
        require(envelope.state.matches.size <= MAX_MATCHES) { "对局数量超过上限" }
        validateState(envelope.state).getOrElse { throw IllegalArgumentException("备份状态无效：${it.message}", it) }
    }

    private fun validateShape(root: JsonObject) {
        requireKeys(root, ENVELOPE_KEYS, "备份")
        val state = requireObject(root.getValue("state"), "state")
        requireKeys(state, STATE_KEYS, "state")
        root.getValue("state").let { stateElement ->
            val stateObject = stateElement as JsonObject
            val profile = stateObject.getValue("profile")
            if (profile !is JsonNull) {
                requireKeys(requireObject(profile, "profile"), PROFILE_KEYS, "profile")
            }
            val matches = stateObject.getValue("matches") as? JsonArray
                ?: throw IllegalArgumentException("state.matches 必须是数组")
            require(matches.size <= MAX_MATCHES) { "对局数量超过上限" }
            matches.forEachIndexed { index, match -> validateMatch(match, index) }
        }
    }

    private fun validateMatch(element: JsonElement, index: Int) {
        val match = requireObject(element, "matches[$index]")
        requireKeys(match, MATCH_KEYS, "matches[$index]")
        val input = match.getValue("input")
        if (input !is JsonNull) requireKeys(requireObject(input, "matches[$index].input"), INPUT_KEYS, "matches[$index].input")
        val legacy = match.getValue("legacy")
        if (legacy !is JsonNull) requireKeys(requireObject(legacy, "matches[$index].legacy"), LEGACY_KEYS, "matches[$index].legacy")
    }

    private fun requireObject(element: JsonElement, label: String): JsonObject =
        element as? JsonObject ?: throw IllegalArgumentException("$label 必须是对象")

    private fun requireKeys(objectValue: JsonObject, expected: Set<String>, label: String) {
        val missing = expected - objectValue.keys
        require(missing.isEmpty()) { "$label 缺少字段：${missing.sorted().joinToString()}" }
        val unknown = objectValue.keys - expected
        require(unknown.isEmpty()) { "$label 包含未知字段：${unknown.sorted().joinToString()}" }
    }

    private fun strictUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException("备份文件不是有效 UTF-8", error)
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024
        const val MAX_MATCHES = 100_000
        val ENVELOPE_KEYS = setOf("format", "schemaVersion", "exportedAtEpochMs", "appVersion", "state")
        val STATE_KEYS = setOf("profile", "matches")
        val PROFILE_KEYS = setOf("id", "name", "initialElo", "targetElo", "targetStartElo", "lastOpponentRank")
        val MATCH_KEYS = setOf(
            "id", "order", "kind", "playedAtEpochMs", "playedZoneId", "outcome", "input", "opponentElo",
            "eloBefore", "delta", "eloAfter", "ruleVersion", "legacy",
        )
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
