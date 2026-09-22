package dev.goelo.android.data

class DogListService(private val store: StateStore) {
    suspend fun add(id: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val normalized = normalize(id)
        require(state.dogIds.none { it.equals(normalized, ignoreCase = true) }) { "野狐 ID 已存在" }
        state.copy(dogIds = state.dogIds + normalized)
    }

    suspend fun remove(id: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val normalized = normalize(id)
        state.copy(dogIds = state.dogIds.filterNot { it.equals(normalized, ignoreCase = true) })
    }

    private fun normalize(value: String): String = value.trim().also {
        require(it.isNotEmpty()) { "野狐 ID 不能为空" }
    }
}
