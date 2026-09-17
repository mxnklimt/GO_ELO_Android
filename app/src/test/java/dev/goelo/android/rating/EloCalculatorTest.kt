package dev.goelo.android.rating
import dev.goelo.android.model.*
import org.junit.Assert.*
import org.junit.Test
class EloCalculatorTest {
 @Test fun knownRankSevenRecord()=assertEquals(2255.321079,opponentElo(RecordInput(7,11,8)),.00001)
 @Test fun endpointsAndRatio(){assertEquals(2800.0,opponentElo(RecordInput(7,1,0)),0.0);assertEquals(1600.0,opponentElo(RecordInput(7,0,20)),0.0);assertEquals(opponentElo(RecordInput(7,3,1)),opponentElo(RecordInput(7,15,5)),0.0)}
 @Test fun equalOpponentsMoveTenPoints(){assertEquals(2010.0,scoreChange(2000.0,2000.0,Outcome.WIN).after,0.0);assertEquals(1990.0,scoreChange(2000.0,2000.0,Outcome.LOSS).after,0.0)}
}
