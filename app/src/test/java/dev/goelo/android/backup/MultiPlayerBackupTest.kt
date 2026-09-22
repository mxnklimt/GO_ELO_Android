package dev.goelo.android.backup

import dev.goelo.android.data.*
import dev.goelo.android.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MultiPlayerBackupTest {
    @Test fun originalV1ArchiveUpgradesWithoutLosingLegacyPrecision() {
        val raw = """{"format":"go-elo-backup","schemaVersion":1,"exportedAtEpochMs":1,"appVersion":"0.1.0","state":{"profile":{"id":"local","name":"甲","initialElo":2000,"targetElo":null,"targetStartElo":null,"lastOpponentRank":7},"matches":[{"id":"old","order":1,"kind":"LEGACY","playedAtEpochMs":null,"playedZoneId":null,"outcome":"LOSS","input":null,"opponentElo":null,"eloBefore":2000,"delta":-12.5,"eloAfter":1987.5,"ruleVersion":"legacy-fixed-v1","legacy":{"fileSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","lineNumber":1,"selfSide":2,"player1":"乙","player2":"甲","sourceResult":1,"sourceDelta":12.5,"charset":"UTF-8"}}]}}"""
        val upgraded=BackupCodec().decode(raw.encodeToByteArray())
        assertEquals(3,upgraded.schemaVersion)
        assertEquals(1,upgraded.state.allProfiles.size)
        assertTrue(upgraded.state.dogIds.isEmpty())
        assertEquals(1987.5,upgraded.state.matches.single().eloAfter,0.0)
        assertEquals("local",upgraded.state.matches.single().playerId)
        assertNull(upgraded.state.matches.single().opponentPlayerId)
        assertEquals(upgraded,BackupCodec().decode(BackupCodec().encode(upgraded)))
    }

    @Test fun fullArchivePreservesBothPlayersSelectionAndSharedGame() = runBlocking {
        val s=MemoryLedger()
        val p=ProfileService(s)
        p.create("甲",2000.0)
        p.add("b","乙",2000.0,s.read().revision)
        MatchService(s).recordKnown("g","b",Outcome.WIN,1,"UTC","local")
        p.select("b",s.read().revision)
        val original=BackupEnvelope(exportedAtEpochMs=1,appVersion="0.3.0",state=s.read().state)
        val restored=BackupCodec().decode(BackupCodec().encode(original))
        assertEquals(original,restored)
        assertEquals("b",restored.state.profile!!.id)
        assertEquals(1990.0,restored.state.forPlayer("b").matches.single().eloAfter,1e-8)
    }

    @Test fun archiveRoundTripsDogIdsAndOldV2ArchiveDefaultsToEmptyList() {
        val state = AppState(Profile(name = "甲", initialElo = 2000.0), emptyList(), dogIds = listOf("AlphaFox", "Beta"))
        val original = BackupEnvelope(exportedAtEpochMs = 1, appVersion = "0.3.0", state = state)
        assertEquals(original, BackupCodec().decode(BackupCodec().encode(original)))

        val v2 = """{"format":"go-elo-backup","schemaVersion":2,"exportedAtEpochMs":1,"appVersion":"0.3.0","state":{"profile":null,"matches":[],"otherProfiles":[]}}"""
        val upgraded = BackupCodec().decode(v2.encodeToByteArray())
        assertEquals(3, upgraded.schemaVersion)
        assertTrue(upgraded.state.dogIds.isEmpty())
    }

    @Test fun invalidReferencesAndTamperedSecondRatingCannotBeRestored() = runBlocking {
        val s=MemoryLedger()
        val p=ProfileService(s)
        p.create("甲",2000.0)
        p.add("b","乙",2000.0,s.read().revision)
        MatchService(s).recordKnown("g","b",Outcome.WIN,1,"UTC","local")
        val state=s.read().state
        val game=state.matches.single()
        listOf(game.copy(opponentPlayerId="missing"), game.copy(opponentPlayerId="local"),
            game.copy(opponentEloAfter=0.0),game.copy(opponentElo=2100.0)).forEach { invalid ->
            assertTrue(runCatching { BackupCodec().encode(BackupEnvelope(exportedAtEpochMs=1,
                appVersion="0.3.0",state=state.copy(matches=listOf(invalid)))) }.isFailure)
        }
    }
}
