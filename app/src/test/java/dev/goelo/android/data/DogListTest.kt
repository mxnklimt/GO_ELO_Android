package dev.goelo.android.data

import dev.goelo.android.model.AppState
import dev.goelo.android.model.Profile
import dev.goelo.android.stats.filterDogIds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DogListTest {
    @Test fun addTrimsIdsRejectsCaseInsensitiveDuplicatesAndRemoves() = runBlocking {
        val store = MemoryLedger(AppState(Profile(name = "棋手", initialElo = 2000.0), emptyList()))
        val service = DogListService(store)

        service.add("  AlphaFox  ", store.read().revision)
        assertEquals(listOf("AlphaFox"), store.read().state.dogIds)
        assertTrue(runCatching { service.add("alphafox", store.read().revision) }.isFailure)

        service.remove("AlphaFox", store.read().revision)
        assertTrue(store.read().state.dogIds.isEmpty())
    }

    @Test fun searchMatchesIdsCaseInsensitivelyAndPreservesOrder() {
        assertEquals(
            listOf("AlphaFox", "fox-master"),
            filterDogIds(listOf("AlphaFox", "Beta", "fox-master"), "FOX"),
        )
        assertEquals(listOf("AlphaFox", "Beta", "fox-master"), filterDogIds(listOf("AlphaFox", "Beta", "fox-master"), ""))
    }
}
