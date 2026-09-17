package dev.goelo.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.memoryStore
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import dev.goelo.android.rating.scoreChange
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryEditTest {
    @Test fun correctingFirstGameChangesLaterScore() = runTest {
        val store = memoryStore()
        try {
            ProfileService(store).create("棋手", 2000.0)
            val service = MatchService(store)
            service.record("a", RecordInput(6, 0, 0), Outcome.WIN, 1000L, "UTC")
            service.record("b", RecordInput(6, 0, 0), Outcome.WIN, 2000L, "UTC")
            service.edit("a", RecordInput(6, 0, 0), Outcome.LOSS, store.read().revision)
            val rows = store.read().state.matches.sortedBy { it.order }
            assertEquals(1990.0, rows[0].eloAfter, 0.0)
            assertEquals(1990.0, rows[1].eloBefore, 0.0)
            assertEquals(scoreChange(1990.0, 2000.0, Outcome.WIN).after, rows[1].eloAfter, 1e-7)
        } finally { store.close() }
    }
}
