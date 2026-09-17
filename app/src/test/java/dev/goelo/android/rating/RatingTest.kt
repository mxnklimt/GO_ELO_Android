package dev.goelo.android.rating

import dev.goelo.android.model.*
import org.junit.Assert.*
import org.junit.Test

class RatingTest {
    @Test fun knownRankSevenRecord() = assertEquals(2255.321079, opponentElo(RecordInput(7,11,8)), .00001)
    @Test fun noSmoothingAtEndpoints() {
        assertEquals(2800.0, opponentElo(RecordInput(7,1,0)), 0.0)
        assertEquals(2800.0, opponentElo(RecordInput(7,20,0)), 0.0)
        assertEquals(1600.0, opponentElo(RecordInput(7,0,20)), 0.0)
        assertEquals(2200.0, opponentElo(RecordInput(7,0,0)), 0.0)
    }
    @Test fun ratioDoesNotDependOnSampleSize() = assertEquals(opponentElo(RecordInput(7,3,1)), opponentElo(RecordInput(7,15,5)), 0.0)
    @Test fun equalOpponentsMoveTenPoints() {
        assertEquals(2010.0, scoreChange(2000.0,2000.0,Outcome.WIN).after,0.0)
        assertEquals(1990.0, scoreChange(2000.0,2000.0,Outcome.LOSS).after,0.0)
    }
    @Test fun parser() {
        assertTrue(parseRecord(7,"20-1").isFailure); assertTrue(parseRecord(7,"-1-3").isFailure); assertTrue(parseRecord(7,"").isFailure)
        assertEquals(RecordInput(7,11,8), parseRecord(7,"11 － 8").getOrThrow())
    }
    @Test fun replayUsesCorrectedPriorResult() {
        val mk = { id: String, order: Long, out: Outcome -> Match(id,order,MatchKind.NATIVE,1,"Asia/Shanghai",out,RecordInput(6,0,0),2000.0,0.0,0.0,"elo-v1") }
        val rs = replay(2000.0,listOf(mk("a",1,Outcome.LOSS),mk("b",2,Outcome.WIN)))
        assertEquals(1990.0,rs[0].eloAfter,0.0); assertEquals(1990.0,rs[1].eloBefore,0.0)
    }
}
