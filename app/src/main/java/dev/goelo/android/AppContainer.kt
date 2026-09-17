package dev.goelo.android

import android.content.Context
import androidx.room.Room
import dev.goelo.android.backup.BackupCodec
import dev.goelo.android.backup.FileSnapshotStore
import dev.goelo.android.backup.RestoreService
import dev.goelo.android.data.GoEloDatabase
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.RoomStateStore
import java.io.File
import java.time.Clock

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, GoEloDatabase::class.java, "go-elo.db").build()
    val stateStore = RoomStateStore(database)
    val matchService = MatchService(stateStore)
    val profileService = ProfileService(stateStore)
    val backupCodec = BackupCodec()
    val snapshotStore = FileSnapshotStore(File(context.noBackupFilesDir, "recovery"), Clock.systemUTC())
    val restoreService = RestoreService(stateStore, snapshotStore, backupCodec, Clock.systemUTC(), "0.1.0")
}
