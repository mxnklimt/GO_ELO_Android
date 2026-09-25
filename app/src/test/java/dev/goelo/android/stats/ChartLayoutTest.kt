package dev.goelo.android.stats

import dev.goelo.android.ui.components.nearestPointIndex
import dev.goelo.android.ui.components.chartScale
import dev.goelo.android.ui.components.visibleMarkerIndices
import dev.goelo.android.ui.components.chartMarkerShape
import dev.goelo.android.ui.components.ChartMarkerShape
import dev.goelo.android.ui.components.chartSelectionStep
import dev.goelo.android.ui.components.chartPointDetail
import dev.goelo.android.model.Outcome
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

    @Test fun `selectedLossKeepsDiamondMarker`() {
        assertEquals(ChartMarkerShape.DIAMOND, chartMarkerShape(Outcome.LOSS))
        assertEquals(ChartMarkerShape.CIRCLE, chartMarkerShape(Outcome.WIN))
    }

    @Test fun `accessiblePointNavigationStopsAtBothEnds`() {
        assertEquals(2, chartSelectionStep(3, 4, -1))
        assertEquals(0, chartSelectionStep(0, 4, -1))
        assertEquals(3, chartSelectionStep(3, 4, 1))
        assertNull(chartSelectionStep(null, 0, 1))
    }

    @Test fun `selectedResultDescriptionIncludesOutcomeScoreAndChange`() {
        val detail = chartPointDetail(SeriesPoint(8, 2204.0, "m8", Outcome.LOSS, -8.0))
        assertTrue(detail.contains("负"))
        assertTrue(detail.contains("2204.0 ELO"))
        assertTrue(detail.contains("-8.0"))
    }
}
