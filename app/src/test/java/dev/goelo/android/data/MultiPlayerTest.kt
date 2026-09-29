package dev.goelo.android.data

import dev.goelo.android.model.*
import dev.goelo.android.rating.validateState
import dev.goelo.android.rating.replayState
import dev.goelo.android.rating.TEMPORARY_ELO_RULE_V3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** The substitute only replaces SQLite; real validation, services and replay run in every mutation. */
class MemoryLedger(initial: AppState = AppState(null, emptyList())) : StateStore {
    private val flow = MutableStateFlow(StoreSnapshot(initial, 0))
    override fun observe() = flow
    override suspend fun read() = flow.value
    override suspend fun update(expectedRevision: Long, transform: (AppState) -> AppState): StoreSnapshot {
        check(flow.value.revision == expectedRevision) { "数据已变化" }
        val next = transform(flow.value.state)
        validateState(next).getOrThrow()
        return StoreSnapshot(next, expectedRevision + 1).also { flow.value = it }
    }
}

class MultiPlayerTest {
    private suspend fun setup(): MemoryLedger = MemoryLedger().also {
        val p = ProfileService(it)
        p.create("甲", 2000.0)
        p.add("b", "乙", 2000.0, it.read().revision)
        p.add("c", "丙", 2000.0, it.read().revision)
    }
    private fun elo(s: AppState, id: String) = s.forPlayer(id).let {
        it.matches.maxByOrNull { m -> m.order }?.eloAfter ?: it.profile!!.initialElo
    }

    @Test fun convertingTemporaryMatchToKnownReplaysBothPlayersAndLaterGames() = runBlocking {
        val store = setup()
        val service = MatchService(store)
        service.record("temp", RecordInput(4, 0, 8), Outcome.WIN, 1, "UTC")
        service.recordKnown("later", "c", Outcome.WIN, 2, "UTC", "b")
        val previous = store.read().state.matches.single { it.id == "temp" }

        service.convertTemporaryToKnown("temp", "b", Outcome.WIN, store.read().revision)

        val state = store.read().state
        val converted = state.matches.single { it.id == "temp" }
        assertEquals(previous.id, converted.id)
        assertEquals(previous.order, converted.order)
        assertEquals(previous.playedAtEpochMs, converted.playedAtEpochMs)
        assertEquals("b", converted.opponentPlayerId)
        assertNull(converted.input)
        assertEquals(10.0, converted.delta, 1e-8)
        assertEquals(1, state.forPlayer("local").matches.count { it.id == "temp" })
        assertEquals(1, state.forPlayer("b").matches.count { it.id == "temp" })
        assertEquals(1990.0, state.matches.single { it.id == "later" }.eloBefore, 1e-8)
        assertEquals(6000.0, state.allProfiles.sumOf { elo(state, it.id) }, 1e-8)
    }

    @Test fun convertingTemporaryMatchRejectsInvalidTargetOrStaleRevisionWithoutMutation() = runBlocking {
        val store = setup()
        val service = MatchService(store)
        service.record("temp", RecordInput(4, 0, 8), Outcome.LOSS, 1, "UTC")
        val before = store.read()
        for (target in listOf("local", "missing")) {
            assertTrue(runCatching { service.convertTemporaryToKnown("temp", target, Outcome.LOSS, before.revision) }.isFailure)
            assertEquals(before, store.read())
        }
        assertTrue(runCatching { service.convertTemporaryToKnown("temp", "b", Outcome.LOSS, before.revision - 1) }.isFailure)
        assertEquals(before, store.read())
    }

    @Test fun oldTemporaryEstimateStaysUntilManuallyEdited() = runBlocking {
        val old = Match("old", 1, MatchKind.NATIVE, 1, "UTC", Outcome.WIN,
            RecordInput(4, 0, 8), null, 0.0, 0.0, 0.0, TEMPORARY_ELO_RULE_V3)
        val store = MemoryLedger(replayState(AppState(Profile(name = "甲", initialElo = 2000.0), listOf(old))))
        val service = MatchService(store)
        val original = store.read().state.matches.single()
        assertEquals(1100.0, original.opponentElo!!, 0.0)
        service.record("new", RecordInput(4, 0, 8), Outcome.WIN, 2, "UTC")
        val afterNewRecord = store.read().state.matches.associateBy { it.id }
        assertEquals(1100.0, afterNewRecord.getValue("old").opponentElo!!, 0.0)
        assertEquals(1595.66856854, afterNewRecord.getValue("new").opponentElo!!, 1e-8)
        service.edit("old", RecordInput(4, 0, 8), Outcome.WIN, store.read().revision)
        val edited = store.read().state.matches.single { it.id == "old" }
        assertEquals(1595.66856854, edited.opponentElo!!, 1e-8)
    }

    @Test fun knownOpponentGetsOppositeDeltaAndOneSharedRecord() = runBlocking {
        val store = setup()
        MatchService(store).recordKnown("g", "b", Outcome.WIN, 1, "UTC", "local")
        val s = store.read().state
        assertEquals(2010.0, elo(s,"local"), 1e-8)
        assertEquals(1990.0, elo(s,"b"), 1e-8)
        assertEquals(2000.0, elo(s,"c"), 1e-8)
        assertEquals(1,s.matches.size)
        val mirrored = s.forPlayer("b").matches.single()
        assertEquals(Outcome.LOSS,mirrored.outcome)
        assertEquals(-10.0,mirrored.delta,1e-8)
        assertEquals("甲",mirrored.opponentName)
        assertEquals("乙",s.forPlayer("local").matches.single().opponentName)
    }

    @Test fun switchingDoesNotMoveHistoryAndTemporaryMatchOnlyUpdatesSelectedPlayer() = runBlocking {
        val store = setup()
        val service = MatchService(store)
        service.recordKnown("g","b",Outcome.WIN,1,"UTC","local")
        ProfileService(store).select("c",store.read().revision)
        service.record("t",RecordInput(6,0,0),Outcome.WIN,2,"UTC")
        val s = store.read().state
        assertEquals("c",s.profile!!.id)
        assertEquals(2012.8012999960577,elo(s,"c"),1e-8)
        assertEquals(2010.0,elo(s,"local"),1e-8)
        assertEquals(1990.0,elo(s,"b"),1e-8)
        assertEquals(listOf("t"),s.forPlayer("c").matches.map { it.id })
        assertEquals(3,s.allProfiles.size)
    }

    @Test fun editingFromSecondSideReplaysLaterGamesAcrossThirdPlayer() = runBlocking {
        val store = setup()
        val service = MatchService(store)
        service.recordKnown("ab","b",Outcome.WIN,1,"UTC","local")
        service.recordKnown("bc","c",Outcome.WIN,2,"UTC","b")
        ProfileService(store).select("b",store.read().revision)
        service.editKnown("ab",Outcome.WIN,store.read().revision)
        val s=store.read().state
        assertEquals(1990.0,elo(s,"local"),1e-8)
        // After reversing AB, B starts BC on 2010 vs C's 2000.
        assertEquals(2019.71225631668,elo(s,"b"),1e-8)
        assertEquals(1990.28774368332,elo(s,"c"),1e-8)
        assertEquals(6000.0,s.allProfiles.sumOf { elo(s,it.id) },1e-8)
    }

    @Test fun deletingEarlierMatchReplaysBothSidesAndLaterOpponents() = runBlocking {
        val store=setup()
        val service=MatchService(store)
        service.recordKnown("ab","b",Outcome.WIN,1,"UTC","local")
        service.recordKnown("bc","c",Outcome.WIN,2,"UTC","b")
        service.delete("ab",store.read().revision)
        val s=store.read().state
        assertEquals(2000.0,elo(s,"local"),1e-8)
        assertEquals(2010.0,elo(s,"b"),1e-8)
        assertEquals(1990.0,elo(s,"c"),1e-8)
    }

    @Test fun undoRemovesBothViewsAndRetryDoesNotDuplicate() = runBlocking {
        val store=setup()
        val service=MatchService(store)
        val receipt=service.recordKnown("ab","b",Outcome.WIN,1,"UTC","local")
        val repeated=service.recordKnown("ab","b",Outcome.WIN,1,"UTC","local")
        assertNull(repeated.undo)
        assertEquals(1,store.read().state.matches.size)
        service.undo(receipt.undo!!)
        val s=store.read().state
        assertTrue(s.matches.isEmpty())
        assertEquals(2000.0,elo(s,"b"),1e-8)
        assertEquals(2000.0,elo(s,"local"),1e-8)
    }

    @Test fun selfMissingOpponentAndReusedRequestAreRejectedWithoutMutation() = runBlocking {
        val store=setup()
        val service=MatchService(store)
        val original=store.read()
        assertTrue(runCatching { service.recordKnown("x","local",Outcome.WIN,1,"UTC","local") }.isFailure)
        assertTrue(runCatching { service.recordKnown("x","missing",Outcome.WIN,1,"UTC","local") }.isFailure)
        assertEquals(original,store.read())
        service.recordKnown("x","b",Outcome.WIN,1,"UTC","local")
        val saved=store.read()
        assertTrue(runCatching { service.recordKnown("x","c",Outcome.WIN,1,"UTC","local") }.isFailure)
        assertEquals(saved,store.read())
    }

    @Test fun renameAndInitialScoreAffectCorrectPlayerAndAllDependentGames() = runBlocking {
        val store=setup()
        MatchService(store).recordKnown("ab","b",Outcome.WIN,1,"UTC","local")
        val profiles=ProfileService(store)
        profiles.select("b",store.read().revision)
        profiles.rename("乙新",store.read().revision)
        profiles.changeInitialElo(2400.0,store.read().revision)
        val s=store.read().state
        assertEquals(2018.181818181818,elo(s,"local"),1e-8)
        assertEquals(2381.818181818182,elo(s,"b"),1e-8)
        assertEquals("乙新",s.forPlayer("local").matches.single().opponentName)
    }

    @Test fun temporaryRetryAndSubsequentKnownGameUseOwnersActualScore() = runBlocking {
        val store=setup()
        val service=MatchService(store)
        service.record("temp",RecordInput(6,0,0),Outcome.WIN,1,"UTC")
        service.record("temp",RecordInput(6,0,0),Outcome.WIN,1,"UTC")
        assertEquals(1,store.read().state.matches.size)
        service.recordKnown("known","b",Outcome.WIN,2,"UTC","local")
        val state=store.read().state
        assertEquals(2022.43301560546,elo(state,"local"),1e-8)
        assertEquals(1990.3682843906022,elo(state,"b"),1e-8)
    }
}
