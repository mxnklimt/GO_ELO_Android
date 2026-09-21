package dev.goelo.android.rating
import dev.goelo.android.model.*
import org.junit.Assert.*
import org.junit.Test
class EloCalculatorTest {
 @Test fun knownRankSevenRecordUsesCurrentRankBaseline()=assertEquals(2313.830270,opponentElo(RecordInput(7,11,8)),.00001)
 @Test fun legacyRuleStillReproducesOldEstimate(){
  val input=RecordInput(7,11,8)
  assertEquals(2255.321079,opponentElo(input,TEMPORARY_ELO_RULE_V1),.00001)
  assertEquals(2213.830270,opponentElo(input,TEMPORARY_ELO_RULE_V2),.00001)
  assertEquals(2313.830270,opponentElo(input,TEMPORARY_ELO_RULE_V3),.00001)
 }
 @Test fun endpointsAndRatio(){assertEquals(2900.0,opponentElo(RecordInput(7,1,0)),0.0);assertEquals(2900.0,opponentElo(RecordInput(7,20,0)),0.0);assertEquals(1700.0,opponentElo(RecordInput(7,0,20)),0.0);assertEquals(2300.0,opponentElo(RecordInput(7,0,0)),0.0);assertEquals(opponentElo(RecordInput(7,3,1)),opponentElo(RecordInput(7,15,5)),0.0)}
 @Test fun equalOpponentsMoveTenPoints(){assertEquals(2010.0,scoreChange(2000.0,2000.0,Outcome.WIN).after,0.0);assertEquals(1990.0,scoreChange(2000.0,2000.0,Outcome.LOSS).after,0.0)}
 @Test fun expectedWinProbabilityUsesExistingEloFormula(){
  assertEquals(0.5,winProbability(2000.0,2000.0),1e-12)
  assertTrue(winProbability(2000.0,2100.0)<0.5)
  assertEquals(1.0,winProbability(2000.0,2100.0)+winProbability(2100.0,2000.0),1e-12)
 }
 @Test fun expectedWinProbabilityRejectsNonFiniteRatings(){
  assertTrue(runCatching { winProbability(Double.NaN,2000.0) }.isFailure)
  assertTrue(runCatching { winProbability(2000.0,Double.POSITIVE_INFINITY) }.isFailure)
 }
}
