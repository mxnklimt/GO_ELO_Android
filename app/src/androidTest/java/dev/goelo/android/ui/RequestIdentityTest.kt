package dev.goelo.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.StateStore
import dev.goelo.android.data.StoreSnapshot
import dev.goelo.android.data.memoryStore
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RequestIdentityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `retry after ambiguous save reuses request identity`() {
        val backing = memoryStore()
        runBlocking { ProfileService(backing).create("小林", 2200.0) }
        val store = ThrowAfterCommitStore(backing)
        val model = AppViewModel(store, MatchService(store), ProfileService(store), AdvancingClock(), { "request-1" })
        compose.setContent { KeepAlive() }
        compose.runOnIdle { model.setRank(7); model.setRecordText("11-8"); model.submit(Outcome.WIN) }
        compose.waitUntil(5_000) { !model.ui.value.busy && model.ui.value.error != null }
        compose.runOnIdle { model.submit(Outcome.WIN) }
        compose.waitUntil(5_000) { !model.ui.value.busy && model.ui.value.snapshot?.state?.matches?.size == 1 }
        val match = model.ui.value.snapshot!!.state.matches.single()
        assertEquals("request-1", match.id)
        assertEquals(1_000L, match.playedAtEpochMs)
        assertEquals("UTC", match.playedZoneId)
    }
}

private class ThrowAfterCommitStore(private val delegate: StateStore) : StateStore {
    private var throwOnce = true
    override fun observe(): Flow<StoreSnapshot> = delegate.observe()
    override suspend fun read(): StoreSnapshot = delegate.read()
    override suspend fun update(expectedRevision: Long, transform: (AppState) -> AppState): StoreSnapshot {
        val saved = delegate.update(expectedRevision, transform)
        if (throwOnce) { throwOnce = false; throw IllegalArgumentException("ambiguous result") }
        return saved
    }
}

private class AdvancingClock : Clock() {
    private var calls = 0
    override fun getZone(): ZoneId = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(if (calls++ == 0) 1_000L else 2_000L)
}

@Composable private fun KeepAlive() = Unit
