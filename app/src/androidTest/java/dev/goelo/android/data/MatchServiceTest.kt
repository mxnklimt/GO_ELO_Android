package dev.goelo.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import dev.goelo.android.model.AppState
import dev.goelo.android.model.LegacyOrigin
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchServiceTest {
    @Test fun retryDoesNotDuplicateAMatch() = runTest {
        val store = memoryStore()
        try {
            ProfileService(store).create("棋手", 2200.0)
            val service = MatchService(store)
            service.record("request-1", RecordInput(7, 0, 0), Outcome.WIN, 1000L, "Asia/Shanghai")
            service.record("request-1", RecordInput(7, 0, 0), Outcome.WIN, 1000L, "Asia/Shanghai")
            assertEquals(1, store.read().state.matches.size)
            assertEquals(2210.0, store.read().state.matches.single().eloAfter, 0.0)
        } finally { store.close() }
    }

    @Test fun editAndDeleteReplayNativeHistory() = runTest {
        val store = memoryStore()
        try {
            ProfileService(store).create("棋手", 2200.0)
            val service = MatchService(store)
            service.record("one", RecordInput(7, 0, 0), Outcome.WIN, 1L, "UTC")
            service.record("two", RecordInput(7, 0, 0), Outcome.WIN, 2L, "UTC")
            val edited = service.edit("one", RecordInput(7, 0, 0), Outcome.LOSS, store.read().revision)
            assertEquals(2200.0, edited.state.matches.last().eloBefore, 0.0)
            val deleted = service.delete("one", edited.revision)
            assertEquals(1, deleted.state.matches.size)
            assertEquals(2200.0, deleted.state.matches.single().eloBefore, 0.0)
        } finally { store.close() }
    }

    @Test fun settingsInvalidateUndoToken() = runTest {
        val store = memoryStore()
        try {
            val profiles = ProfileService(store)
            profiles.create("棋手", 2200.0)
            val receipt = MatchService(store).record("one", RecordInput(7, 0, 0), Outcome.WIN, 1L, "UTC")
            profiles.rename("新名", store.read().revision)
            assertTrue(runCatching { MatchService(store).undo(requireNotNull(receipt.undo)) }.isFailure)
        } finally { store.close() }
    }

    @Test fun legacyHistoryCannotBeEdited() = runTest {
        val store = memoryStore()
        try {
            val legacy = Match("legacy", 1, MatchKind.LEGACY, null, null, Outcome.WIN, null, null,
                2200.0, 10.0, 2210.0, "legacy-fixed-v1",
                LegacyOrigin("a".repeat(64), 1, 1, "我", "甲", 1, 10.0, "UTF-8"))
            store.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), listOf(legacy)) }
            assertTrue(runCatching { MatchService(store).edit("legacy", RecordInput(7, 0, 0), Outcome.LOSS, store.read().revision) }.isFailure)
        } finally { store.close() }
    }
}
