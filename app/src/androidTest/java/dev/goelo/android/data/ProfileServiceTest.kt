package dev.goelo.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProfileServiceTest {
    @Test fun targetRecordsStartingEloAndCanBeCleared() = runTest {
        val store = memoryStore()
        try {
            val service = ProfileService(store)
            service.create("棋手", 2200.0)
            val target = service.setTarget(2300.0, store.read().revision)
            assertEquals(2200.0, target.state.profile!!.targetStartElo, 0.0)
            val cleared = service.setTarget(null, target.revision)
            assertNull(cleared.state.profile!!.targetElo)
            assertNull(cleared.state.profile!!.targetStartElo)
        } finally { store.close() }
    }

    @Test fun changingInitialEloReplaysNativeHistory() = runTest {
        val store = memoryStore()
        try {
            val service = ProfileService(store)
            service.create("棋手", 2200.0)
            val updated = service.changeInitialElo(2400.0, store.read().revision)
            assertEquals(2400.0, updated.state.profile!!.initialElo, 0.0)
        } finally { store.close() }
    }

    @Test fun settingsSurviveOpeningTheDatabaseAgain() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "go-elo-settings-${UUID.randomUUID()}"
        val first = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
        try {
            val service = ProfileService(first)
            service.create("旧名", 2000.0)
            val renamed = service.rename("新名", first.read().revision)
            service.setTarget(2200.0, renamed.revision)
            first.close()

            val reopened = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name).allowMainThreadQueries().build())
            try {
                val profile = reopened.read().state.profile!!
                assertEquals("新名", profile.name)
                assertEquals(2200.0, profile.targetElo!!, 0.0)
            } finally { reopened.close() }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
