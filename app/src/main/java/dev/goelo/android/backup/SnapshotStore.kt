package dev.goelo.android.backup

data class LocalSnapshot(val id: String, val createdAtEpochMs: Long)

interface SnapshotStore {
    suspend fun save(bytes: ByteArray): LocalSnapshot
    suspend fun list(): List<LocalSnapshot>
    suspend fun read(id: String): ByteArray
}
