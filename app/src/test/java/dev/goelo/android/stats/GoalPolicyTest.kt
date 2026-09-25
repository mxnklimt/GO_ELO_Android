package dev.goelo.android.stats

import dev.goelo.android.model.Profile
import org.junit.Assert.*
import org.junit.Test

class GoalPolicyTest {
    private val profile = Profile(name = "x", initialElo = 2245.0)

    @Test fun referenceRankUsesIntervalsCenteredOnBaselines() {
        val cases = listOf(
            999.9 to "1 段以下", 1000.0 to "1 段", 1199.9 to "1 段",
            1200.0 to "2 段", 2199.9 to "6 段", 2200.0 to "7 段",
            2245.0 to "7 段", 2399.9 to "7 段", 2400.0 to "8 段",
            2600.0 to "9 段", 2799.9 to "9 段", 2800.0 to "9 段基准以上",
        )
        cases.forEach { (elo, expected) -> assertEquals("ELO $elo", expected, goalView(profile, elo).rankLabel) }
    }

    @Test fun defaultGoalIsTheNextRankEntryBoundary() {
        val below = goalView(profile, 999.0)
        assertEquals(1000.0, below.target!!, 0.0)
        val sevenDan = goalView(profile, 2245.0)
        assertEquals(2400.0, sevenDan.target!!, 0.0)
        assertEquals(155.0, sevenDan.remaining!!, 0.0)
        assertEquals(0.225, sevenDan.progress!!, 0.000001)
        assertEquals(2600.0, goalView(profile, 2400.0).target!!, 0.0)
        assertNull(goalView(profile, 2600.0).target)
    }

    @Test fun customGoalStillUsesItsStoredStartingScore() {
        val v = goalView(Profile(name="x", initialElo=2000.0, targetElo=2100.0, targetStartElo=2000.0), 2150.0)
        assertTrue(v.achieved); assertEquals(1.0, v.progress!!, 0.0); assertEquals(0.0, v.remaining!!, 0.0)
    }
}
