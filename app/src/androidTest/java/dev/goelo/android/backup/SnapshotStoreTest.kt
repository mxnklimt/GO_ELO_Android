package dev.goelo.android.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotStoreTest {
    @Test fun retainsOnlyThreeSnapshotsAndRejectsUnknownId() = runTest {
        val root = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "snapshot-test-${UUID.randomUUID()}")
        try {
            val clock = Clock.fixed(Instant.ofEpochMilli(1000L), ZoneOffset.UTC)
            val store = FileSnapshotStore(root, clock)
            repeat(4) { store.save("$it".encodeToByteArray()) }
            assertEquals(3, store.list().size)
            assertTrue(runCatching { store.read("../../outside") }.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }
}
