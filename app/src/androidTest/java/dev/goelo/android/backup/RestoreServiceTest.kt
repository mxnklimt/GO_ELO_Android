package dev.goelo.android.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.memoryStore
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Profile
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreServiceTest {
    private val failingSnapshots = object : SnapshotStore {
        override suspend fun save(bytes: ByteArray): LocalSnapshot = throw IOException("disk full")
        override suspend fun list(): List<LocalSnapshot> = emptyList()
        override suspend fun read(id: String): ByteArray = throw FileNotFoundException(id)
    }

    @Test fun cannotReplaceWhenSafetySnapshotFails() = runTest {
        val store = memoryStore()
        try {
            ProfileService(store).create("原档案", 2000.0)
            val before = store.read()
            val restore = RestoreService(store, failingSnapshots, BackupCodec(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), "0.1.0")
            val prepared = restore.prepareState(AppState(Profile(name = "新档案", initialElo = 2200.0), emptyList()), "备份文件")
            assertTrue(runCatching { restore.apply(prepared) }.isFailure)
            assertEquals(before, store.read())
        } finally {
            store.close()
        }
    }

    @Test fun stalePreviewCannotReplaceCurrentState() = runTest {
        val store = memoryStore()
        val snapshots = object : SnapshotStore {
            override suspend fun save(bytes: ByteArray) = LocalSnapshot("one", 0)
            override suspend fun list() = emptyList<LocalSnapshot>()
            override suspend fun read(id: String) = byteArrayOf()
        }
        try {
            ProfileService(store).create("原档案", 2000.0)
            val restore = RestoreService(store, snapshots, BackupCodec(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), "0.1.0")
            val prepared = restore.prepareState(AppState(Profile(name = "新档案", initialElo = 2200.0), emptyList()), "备份文件")
            ProfileService(store).rename("变化后的档案", store.read().revision)
            assertTrue(runCatching { restore.apply(prepared) }.isFailure)
            assertEquals("变化后的档案", store.read().state.profile!!.name)
        } finally {
            store.close()
        }
    }
}
