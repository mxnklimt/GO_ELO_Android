package dev.goelo.android.data

import dev.goelo.android.model.AppState
import kotlinx.coroutines.flow.Flow

data class StoreSnapshot(val state: AppState, val revision: Long)

interface StateStore {
    fun observe(): Flow<StoreSnapshot>
    suspend fun read(): StoreSnapshot
    suspend fun update(expectedRevision: Long, transform: (AppState) -> AppState): StoreSnapshot
}
