package dev.goelo.android.backup

import androidx.core.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Internal recovery snapshots. They live below noBackupFilesDir and are never user-selected paths. */
class FileSnapshotStore(
    private val root: File,
    private val clock: Clock,
) : SnapshotStore {
    override suspend fun save(bytes: ByteArray): LocalSnapshot = withContext(Dispatchers.IO) {
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "恢复快照超过 32 MiB 上限" }
        root.mkdirs()
        check(root.isDirectory) { "无法创建恢复快照目录" }
        cleanInterruptedFiles()
        val createdAt = clock.millis()
        val snapshot = LocalSnapshot("${UUID.randomUUID()}-$createdAt", createdAt)
        val target = fileFor(snapshot.id)
        val atomic = AtomicFile(target)
        var stream: FileOutputStream? = atomic.startWrite()
        try {
            stream!!.write(bytes)
            stream!!.fd.sync()
            atomic.finishWrite(stream)
            stream = null
        } catch (error: IOException) {
            atomic.failWrite(stream)
            throw error
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
        pruneToLatestThree()
        snapshot
    }

    override suspend fun list(): List<LocalSnapshot> = withContext(Dispatchers.IO) {
        cleanInterruptedFiles()
        root.listFiles()
            .orEmpty()
            .mapNotNull(::snapshotFromFile)
            .sortedWith(compareByDescending<LocalSnapshot> { it.createdAtEpochMs }.thenByDescending { it.id })
    }

    override suspend fun read(id: String): ByteArray = withContext(Dispatchers.IO) {
        val snapshot = list().firstOrNull { it.id == id }
            ?: throw FileNotFoundException("找不到恢复快照")
        val bytes = fileFor(snapshot.id).readBytes()
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "恢复快照超过 32 MiB 上限" }
        bytes
    }

    private fun pruneToLatestThree() {
        root.listFiles()
            .orEmpty()
            .mapNotNull { file -> snapshotFromFile(file)?.let { it to file } }
            .sortedWith(compareByDescending<Pair<LocalSnapshot, File>> { it.first.createdAtEpochMs }.thenByDescending { it.first.id })
            .drop(MAX_SNAPSHOTS)
            .forEach { (_, file) -> file.delete() }
    }

    private fun cleanInterruptedFiles() {
        if (!root.exists()) return
        root.listFiles()?.forEach { file ->
            if (file.isFile && snapshotFromFile(file) == null) file.delete()
        }
    }

    private fun snapshotFromFile(file: File): LocalSnapshot? {
        val match = FILE_PATTERN.matchEntire(file.name) ?: return null
        val id = match.groupValues[1]
        val createdAt = id.substringAfterLast('-').toLongOrNull() ?: return null
        return LocalSnapshot(id, createdAt)
    }

    private fun fileFor(id: String): File = File(root, "$id.goelo")

    private companion object {
        const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024
        const val MAX_SNAPSHOTS = 3
        val FILE_PATTERN = Regex("([0-9a-fA-F-]{36}-\\d+)\\.goelo")
    }
}
