package dev.goelo.android.data

import dev.goelo.android.model.*

class ProfileService(private val store: StateStore) {
    suspend fun create(name: String, initialElo: Double): StoreSnapshot = store.update(0) { state ->
        require(state.profile == null && state.matches.isEmpty()) { "档案已存在" }
        AppState(Profile(name = validName(name), initialElo = validElo(initialElo)), emptyList())
    }

    suspend fun add(id: String, name: String, initialElo: Double, expectedRevision: Long): StoreSnapshot =
        store.update(expectedRevision) { state ->
            require(state.profile != null) { "请先创建档案" }
            require(id.isNotBlank() && state.allProfiles.none { it.id == id }) { "棋手 ID 重复或无效" }
            state.copy(otherProfiles = state.otherProfiles + Profile(id = id, name = validName(name), initialElo = validElo(initialElo)))
        }

    suspend fun select(id: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val selected = requireNotNull(state.allProfiles.find { it.id == id }) { "未找到棋手" }
        state.copy(profile = selected, otherProfiles = state.allProfiles.filter { it.id != id })
    }

    suspend fun rename(name: String, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        state.copy(profile = requireNotNull(state.profile).copy(name = validName(name)))
    }

    suspend fun changeInitialElo(value: Double, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val profile = requireNotNull(state.profile)
        require(state.matches.none { it.playerId == profile.id && it.kind == MatchKind.LEGACY }) { "该棋手含旧历史，不能直接调整衔接分" }
        rehydrated(state.copy(profile = profile.copy(initialElo = validElo(value))))
    }

    suspend fun setTarget(value: Double?, expectedRevision: Long): StoreSnapshot = store.update(expectedRevision) { state ->
        val profile = requireNotNull(state.profile)
        val changed = if (value == null) profile.copy(targetElo = null, targetStartElo = null) else {
            require(value.isFinite() && value > currentElo(state)) { "目标必须高于当前分" }
            profile.copy(targetElo = value, targetStartElo = currentElo(state))
        }
        state.copy(profile = changed)
    }

    private fun validName(value: String): String = value.trim().also { require(it.isNotEmpty()) { "名称不能为空" } }
    private fun validElo(value: Double): Double = value.also { require(it.isFinite() && it > 0) { "初始分必须大于 0" } }
}
