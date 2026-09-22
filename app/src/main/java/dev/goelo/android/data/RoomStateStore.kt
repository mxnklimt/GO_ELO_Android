package dev.goelo.android.data

import androidx.room.withTransaction
import dev.goelo.android.model.AppState
import dev.goelo.android.rating.validateState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStateStore(private val database: GoEloDatabase) : StateStore {
    private val dao = database.stateDao()

    override fun observe(): Flow<StoreSnapshot> = dao.observeRevision().map { read() }

    override suspend fun read(): StoreSnapshot = database.withTransaction { readSnapshot() }

    override suspend fun update(expectedRevision: Long, transform: (AppState) -> AppState): StoreSnapshot =
        database.withTransaction {
            val current = readSnapshot()
            check(current.revision == expectedRevision) { "数据已变化，请重新打开操作" }
            val next = transform(current.state)
            validateState(next).getOrThrow()
            writeState(next, current.revision + 1)
            StoreSnapshot(next, current.revision + 1)
        }

    internal suspend fun readSnapshot(): StoreSnapshot {
        val meta = dao.meta()
        val profiles = dao.profiles().map { it.toModel() }
        val selected = profiles.find { it.id == (meta?.activePlayerId ?: "local") }
        check(profiles.isEmpty() || selected != null) { "当前棋手档案缺失" }
        return StoreSnapshot(AppState(selected, dao.matches().map { it.toModel() },
            profiles.filter { it.id != selected?.id }, dao.dogIds()), meta?.revision ?: 0L)
    }

    private suspend fun writeState(state: AppState, revision: Long) {
        dao.clearMatches()
        if (state.matches.isNotEmpty()) dao.putMatches(state.matches.map { it.toEntity() })
        dao.clearDogIds()
        if (state.dogIds.isNotEmpty()) dao.putDogIds(state.dogIds.map { DogIdEntity(it) })
        dao.clearProfile()
        state.allProfiles.forEach { dao.putProfile(it.toEntity()) }
        dao.putMeta(MetaEntity(revision = revision, activePlayerId = state.profile?.id ?: "local"))
    }

    fun close() = database.close()
}
