package dev.goelo.android.stats

import dev.goelo.android.ui.components.nearestPointIndex
import dev.goelo.android.ui.components.chartScale
import dev.goelo.android.ui.components.visibleMarkerIndices
import org.junit.Assert.*
import org.junit.Test

class ChartLayoutTest {
    @Test fun `returns null for an empty or unmeasurable chart`() {
        assertNull(nearestPointIndex(10f, 100f, 0))
        assertNull(nearestPointIndex(10f, 0f, 3))
    }

    @Test fun `centers a single point and clamps multiple points`() {
        assertEquals(0, nearestPointIndex(80f, 100f, 1))
        assertEquals(0, nearestPointIndex(-10f, 100f, 5))
        assertEquals(2, nearestPointIndex(51f, 100f, 5))
        assertEquals(4, nearestPointIndex(200f, 100f, 5))
    }

    @Test fun `scaleShowsScoreTicksAndOnlyRelevantRankBoundary`() {
        val scale = chartScale(listOf(SeriesPoint(0, 2180.0, null), SeriesPoint(8, 2245.0, "m8")))
        assertTrue(scale.lower < 2180.0)
        assertTrue(scale.upper > 2245.0)
        assertTrue(scale.ticks.all { it in scale.lower..scale.upper })
        assertTrue(scale.ticks.size in 2..5)
        assertEquals(listOf(7 to 2200.0), scale.rankEntries.map { it.rank to it.elo })
    }

    @Test fun `longSeriesThinsPermanentNodesButKeepsLatest`() {
        assertEquals((1..20).toList(), visibleMarkerIndices(21))
        val dense = visibleMarkerIndices(101)
        assertTrue(dense.size < 21)
        assertFalse(dense.contains(0))
        assertEquals(100, dense.last())
    }
}
