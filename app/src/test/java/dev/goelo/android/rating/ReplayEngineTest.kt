package dev.goelo.android.rating
import dev.goelo.android.model.*
import org.junit.Assert.*
import org.junit.Test
class ReplayEngineTest {
 private fun native(id:String,order:Long)=Match(id,order,MatchKind.NATIVE,1L,"UTC",Outcome.WIN,RecordInput(6,0,0),2000.0,2000.0,0.0,2000.0,"elo-v1")
 @Test fun legacyTemporaryRuleIsUsedWhenReplaying(){
  val input=RecordInput(7,11,8)
  val expected=opponentElo(input,TEMPORARY_ELO_RULE_V1)
  val r=replay(2000.0,listOf(Match("old",1,MatchKind.NATIVE,1L,"UTC",Outcome.WIN,input,expected,2000.0,0.0,2000.0,TEMPORARY_ELO_RULE_V1)))
  assertEquals(expected,r.single().opponentElo!!,0.00001)
 }
 @Test fun legacyThenNativeKeepsExactDelta(){val l=Match("l",1,MatchKind.LEGACY,null,null,Outcome.WIN,null,null,0.0,12.34,12.34,"legacy-fixed-v1",LegacyOrigin("a".repeat(64),1,1,"a","b",1,12.34,"UTF-8"));val r=replay(2000.0,listOf(l,native("n",2)));assertEquals(12.34,r[0].delta,0.0);assertEquals(2012.34,r[1].eloBefore,0.0)}
 @Test fun correctedPriorResultFeedsNextMatch(){val first=native("a",1).copy(outcome=Outcome.LOSS);val second=native("b",2);val r=replay(2000.0,listOf(first,second));assertEquals(1990.0,r[0].eloAfter,0.0);assertEquals(1990.0,r[1].eloBefore,0.0)}
 @Test fun rejectsMalformedState(){val p=Profile(name="x",initialElo=2000.0);assertTrue(validateState(AppState(p,listOf(native("x",1),native("x",2)))).isFailure);assertTrue(validateState(AppState(p,listOf(native("x",1).copy(eloBefore=Double.NaN)))).isFailure);assertTrue(validateState(AppState(p,listOf(native("x",1).copy(ruleVersion="bad")))).isFailure)}
}
