package dev.goelo.android.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

fun memoryStore(): RoomStateStore = RoomStateStore(
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        GoEloDatabase::class.java,
    ).allowMainThreadQueries().build(),
)
