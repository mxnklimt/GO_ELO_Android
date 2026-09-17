package dev.goelo.android.stats

import dev.goelo.android.model.Profile
import org.junit.Assert.*
import org.junit.Test

class GoalPolicyTest {
    @Test fun boundariesAndCustomGoal() {
        assertEquals("1 段以下", goalView(Profile(name="x", initialElo=999.0), 999.0).rankLabel)
        assertEquals(1000.0, goalView(Profile(name="x", initialElo=999.0), 999.0).target!!, 0.0)
        assertEquals("9 段参考", goalView(Profile(name="x", initialElo=2600.0), 2600.0).rankLabel)
        assertEquals("9 段基准以上", goalView(Profile(name="x", initialElo=2700.0), 2700.0).rankLabel)
        val v = goalView(Profile(name="x", initialElo=2000.0, targetElo=2100.0, targetStartElo=2000.0), 2150.0)
        assertTrue(v.achieved); assertEquals(1.0, v.progress!!, 0.0); assertEquals(0.0, v.remaining!!, 0.0)
    }
}
