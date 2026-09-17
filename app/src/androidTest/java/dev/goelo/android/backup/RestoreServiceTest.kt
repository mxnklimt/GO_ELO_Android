package dev.goelo.android.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.StateStore
import dev.goelo.android.data.StoreSnapshot
import dev.goelo.android.data.memoryStore
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Profile
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    @Test fun transactionFailureLeavesStateInPlaceAfterSnapshot() = runTest {
        val before = StoreSnapshot(AppState(Profile(name = "原档案", initialElo = 2000.0), emptyList()), 4L)
        val failingUpdate = object : StateStore {
            override fun observe(): Flow<StoreSnapshot> = flowOf(before)
            override suspend fun read(): StoreSnapshot = before
            override suspend fun update(expectedRevision: Long, transform: (AppState) -> AppState): StoreSnapshot =
                throw IllegalStateException("transaction failed")
        }
        val saved = mutableListOf<ByteArray>()
        val snapshots = object : SnapshotStore {
            override suspend fun save(bytes: ByteArray): LocalSnapshot { saved += bytes; return LocalSnapshot("one", 0) }
            override suspend fun list() = emptyList<LocalSnapshot>()
            override suspend fun read(id: String) = byteArrayOf()
        }
        val restore = RestoreService(failingUpdate, snapshots, BackupCodec(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), "0.1.0")
        val prepared = restore.prepareState(AppState(Profile(name = "新档案", initialElo = 2200.0), emptyList()), "备份文件")
        assertTrue(runCatching { restore.apply(prepared) }.isFailure)
        assertEquals(before, failingUpdate.read())
        assertEquals(1, saved.size)
    }

    @Test fun applyingSameBackupTwiceReplacesInsteadOfStacking() = runTest {
        val store = memoryStore()
        val snapshots = object : SnapshotStore {
            val saved = mutableListOf<ByteArray>()
            override suspend fun save(bytes: ByteArray): LocalSnapshot { saved += bytes; return LocalSnapshot(saved.size.toString(), 0) }
            override suspend fun list() = emptyList<LocalSnapshot>()
            override suspend fun read(id: String) = byteArrayOf()
        }
        try {
            ProfileService(store).create("原档案", 2000.0)
            val codec = BackupCodec()
            val bytes = codec.encode(BackupEnvelope(1L, "0.1.0", AppState(Profile(name = "恢复档案", initialElo = 2200.0), emptyList())))
            val restore = RestoreService(store, snapshots, codec, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), "0.1.0")
            restore.apply(restore.prepare(bytes, "same.goelo"))
            restore.apply(restore.prepare(bytes, "same.goelo"))
            assertEquals("恢复档案", store.read().state.profile!!.name)
            assertTrue(store.read().state.matches.isEmpty())
            assertEquals(2, snapshots.saved.size)
        } finally {
            store.close()
        }
    }
}
