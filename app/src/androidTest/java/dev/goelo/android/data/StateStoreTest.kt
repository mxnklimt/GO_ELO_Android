package dev.goelo.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Profile
import kotlinx.coroutines.test.runTest
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
}
