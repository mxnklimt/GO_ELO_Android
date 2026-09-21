package dev.goelo.android.data

import dev.goelo.android.model.*
import dev.goelo.android.rating.validateState
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
