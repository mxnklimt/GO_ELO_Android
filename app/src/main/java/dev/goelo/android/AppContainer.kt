package dev.goelo.android

import android.content.Context
import androidx.room.Room
import dev.goelo.android.data.GoEloDatabase
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.RoomStateStore

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, GoEloDatabase::class.java, "go-elo.db").build()
    val stateStore = RoomStateStore(database)
    val matchService = MatchService(stateStore)
    val profileService = ProfileService(stateStore)
}
