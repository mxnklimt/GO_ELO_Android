package dev.goelo.android.stats

import dev.goelo.android.ui.components.nearestPointIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
