package dev.goelo.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.goelo.android.model.AppState
import dev.goelo.android.model.LegacyOrigin
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
import dev.goelo.android.model.RecordInput
import dev.goelo.android.rating.scoreChange
import kotlinx.coroutines.test.runTest
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateStoreTest {
    @Test fun failedTransformRollsBackAtomically() = runTest {
        val store = memoryStore()
        try {
            val first = store.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), emptyList()) }
            assertTrue(runCatching { store.update(first.revision) { error("boom") } }.isFailure)
            assertEquals(first, store.read())
        } finally { store.close() }
    }

    @Test fun staleRevisionIsRejected() = runTest {
        val store = memoryStore()
        try {
            val first = store.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), emptyList()) }
            assertTrue(runCatching { store.update(0) { first.state } }.isFailure)
            assertEquals(first, store.read())
        } finally { store.close() }
    }

    @Test fun rejectsDuplicatePlayerIds() = runTest {
        val store = memoryStore()
        try {
            val invalid = AppState(Profile(id = "same", name = "棋手", initialElo = 2200.0), emptyList(),
                listOf(Profile(id = "same", name = "另一位", initialElo = 2000.0)))
            assertTrue(runCatching { store.update(0) { invalid } }.isFailure)
            assertEquals(0L, store.read().revision)
        } finally { store.close() }
    }

    @Test fun rejectsLegacyOrderedAfterNative() = runTest {
        val store = memoryStore()
        try {
            val legacy = Match("l", 2, MatchKind.LEGACY, null, null, Outcome.WIN, null, null,
                2200.0, 10.0, 2210.0, "legacy-fixed-v1",
                LegacyOrigin("a".repeat(64), 1, 1, "我", "甲", 1, 10.0, "UTF-8"))
            val score = scoreChange(2210.0, 2200.0, Outcome.WIN)
            val native = Match("n", 1, MatchKind.NATIVE, 1L, "UTC", Outcome.WIN,
                RecordInput(7, 0, 0), 2200.0, score.before, score.delta, score.after, "elo-v1")
            assertTrue(runCatching {
                store.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), listOf(native, legacy)) }
            }.isFailure)
        } finally { store.close() }
    }

    @Test fun fileBackedRoomSurvivesReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "state-store-${UUID.randomUUID()}.db"
        val first = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
        try {
            first.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), emptyList()) }
        } finally { first.close() }
        val reopened = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
        try {
            assertEquals("棋手", reopened.read().state.profile!!.name)
            assertEquals(1L, reopened.read().revision)
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun dogIdsSurviveRoomReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "dog-list-${UUID.randomUUID()}.db"
        val first = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
        try {
            first.update(0) { AppState(Profile(name = "棋手", initialElo = 2200.0), emptyList(), dogIds = listOf("AlphaFox", "Beta")) }
        } finally { first.close() }
        val reopened = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
        try { assertEquals(listOf("AlphaFox", "Beta"), reopened.read().state.dogIds) }
        finally { reopened.close(); context.deleteDatabase(name) }
    }
}
