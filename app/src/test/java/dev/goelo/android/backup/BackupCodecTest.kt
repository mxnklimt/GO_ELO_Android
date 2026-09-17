package dev.goelo.android.backup

import dev.goelo.android.model.AppState
import dev.goelo.android.model.LegacyOrigin
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
import dev.goelo.android.model.RecordInput
import dev.goelo.android.rating.scoreChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test fun emptyArchiveRoundTripsWithoutInventedProfile() {
        val original = BackupEnvelope(exportedAtEpochMs = 1000L, appVersion = "0.1.0", state = AppState(null, emptyList()))
        val codec = BackupCodec()
        assertEquals(original, codec.decode(codec.encode(original)))
    }

    @Test fun legacyArchivePreservesUnknownDateAndExactDelta() {
        val legacy = Match(
            id = "legacy-1", order = 1, kind = MatchKind.LEGACY, playedAtEpochMs = null, playedZoneId = null,
            outcome = Outcome.LOSS, input = null, opponentElo = null, eloBefore = 2000.0, delta = -12.5,
            eloAfter = 1987.5, ruleVersion = "legacy-fixed-v1",
            legacy = LegacyOrigin("a".repeat(64), 7, 2, "甲", "乙", 1, 12.5, "GB18030"),
        )
        val original = BackupEnvelope(1000L, "0.1.0", state = AppState(Profile(name = "棋手", initialElo = 2000.0), matches = listOf(legacy)))
        assertEquals(original, BackupCodec().decode(BackupCodec().encode(original)))
    }

    @Test fun unknownSchemaIsRejected() {
        val raw = """{"format":"go-elo-backup","schemaVersion":99,"exportedAtEpochMs":1000,"appVersion":"9","state":{"profile":null,"matches":[]}}"""
        assertTrue(runCatching { BackupCodec().decode(raw.encodeToByteArray()) }.isFailure)
    }

    @Test fun missingNullableOrLegacyKeysAreRejected() {
        val missingProfileField = """{"format":"go-elo-backup","schemaVersion":1,"exportedAtEpochMs":1,"appVersion":"1","state":{"profile":{"id":"local","name":"x","initialElo":2000,"targetElo":null,"targetStartElo":null},"matches":[]}}"""
        val missingLegacyField = """{"format":"go-elo-backup","schemaVersion":1,"exportedAtEpochMs":1,"appVersion":"1","state":{"profile":{"id":"local","name":"x","initialElo":2000,"targetElo":null,"targetStartElo":null,"lastOpponentRank":7},"matches":[{"id":"l","order":1,"kind":"LEGACY","playedAtEpochMs":null,"playedZoneId":null,"outcome":"WIN","input":null,"opponentElo":null,"eloBefore":2000,"delta":1,"eloAfter":2001,"ruleVersion":"legacy-fixed-v1","legacy":{"fileSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","lineNumber":1,"selfSide":1,"player1":"a","player2":"b","sourceResult":1,"sourceDelta":1}}]}}"""
        val codec = BackupCodec()
        assertTrue(runCatching { codec.decode(missingProfileField.encodeToByteArray()) }.isFailure)
        assertTrue(runCatching { codec.decode(missingLegacyField.encodeToByteArray()) }.isFailure)
    }

    @Test fun malformedUtf8AndNonFiniteNumbersAreRejected() {
        assertTrue(runCatching { BackupCodec().decode(byteArrayOf(0xC3.toByte())) }.isFailure)
        val raw = """{"format":"go-elo-backup","schemaVersion":1,"exportedAtEpochMs":1,"appVersion":"1","state":{"profile":{"id":"local","name":"x","initialElo":NaN,"targetElo":null,"targetStartElo":null,"lastOpponentRank":7},"matches":[]}}"""
        assertTrue(runCatching { BackupCodec().decode(raw.encodeToByteArray()) }.isFailure)
    }

    @Test fun truncatedArchiveIsRejected() {
        val bytes = BackupCodec().encode(BackupEnvelope(1L, "1", AppState(null, emptyList())))
        assertTrue(runCatching { BackupCodec().decode(bytes.copyOf(bytes.size - 1)) }.isFailure)
    }

    @Test fun semanticMatchErrorsIdentifyIndexedField() {
        val invalid = legacy("a", 1).copy(ruleVersion = "not-a-rule")
        val error = runCatching {
            BackupCodec().encode(BackupEnvelope(1L, "1", AppState(Profile(name = "棋手", initialElo = 2000.0), listOf(invalid))))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("matches[0].ruleVersion"))
    }

    @Test fun duplicateIdsAndLegacyAfterNativeAreRejected() {
        val profile = Profile(name = "棋手", initialElo = 2000.0)
        assertTrue(runCatching {
            BackupCodec().encode(BackupEnvelope(1L, "1", AppState(profile, listOf(legacy("same", 1), legacy("same", 2)))))
        }.exceptionOrNull()?.message.orEmpty().contains("matches[1].id"))

        val score = scoreChange(2000.0, 2000.0, Outcome.WIN)
        val native = Match("native", 1, MatchKind.NATIVE, 1L, "UTC", Outcome.WIN, RecordInput(6, 0, 0),
            2000.0, score.before, score.delta, score.after, "elo-v1")
        val error = runCatching {
            BackupCodec().encode(BackupEnvelope(1L, "1", AppState(profile, listOf(native, legacy("legacy", 2)))))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("旧历史不能排在"))
    }

    private fun legacy(id: String, order: Long) = Match(
        id, order, MatchKind.LEGACY, null, null, Outcome.WIN, null, null, 2000.0, 10.0, 2010.0,
        "legacy-fixed-v1", LegacyOrigin("a".repeat(64), 1, 1, "甲", "乙", 1, 10.0, "UTF-8"),
    )
}
