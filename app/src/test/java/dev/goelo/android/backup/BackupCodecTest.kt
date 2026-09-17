package dev.goelo.android.backup

import dev.goelo.android.model.AppState
import dev.goelo.android.model.LegacyOrigin
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
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
}
