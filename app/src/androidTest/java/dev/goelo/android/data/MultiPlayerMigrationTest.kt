package dev.goelo.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.goelo.android.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MultiPlayerMigrationTest {
    @Test fun upgradeV1PreservesRecordThenPersistsBothPlayersAcrossReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE profile (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, initialElo REAL NOT NULL, targetElo REAL, targetStartElo REAL, lastOpponentRank INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE matches (id TEXT NOT NULL PRIMARY KEY, orderIndex INTEGER NOT NULL, kind TEXT NOT NULL, playedAtEpochMs INTEGER, playedZoneId TEXT, outcome TEXT NOT NULL, opponentRank INTEGER, recordWins INTEGER, recordLosses INTEGER, opponentElo REAL, eloBefore REAL NOT NULL, delta REAL NOT NULL, eloAfter REAL NOT NULL, ruleVersion TEXT NOT NULL, legacyJson TEXT)")
            db.execSQL("CREATE UNIQUE INDEX index_matches_orderIndex ON matches (orderIndex)")
            db.execSQL("CREATE TABLE meta (id INTEGER NOT NULL PRIMARY KEY, revision INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table VALUES (42, 'a52ea2dc26097c6bc36b846d27eb64a3')")
            db.execSQL("INSERT INTO profile VALUES ('local', '甲', 2000, null, null, 6)")
            db.execSQL("INSERT INTO matches VALUES ('old', 1, 'NATIVE', 1, 'UTC', 'WIN', 6, 0, 0, 2000, 2000, 10, 2010, 'elo-v1', null)")
            db.execSQL("INSERT INTO meta VALUES (0, 2)")
            db.version = 1
        }
        fun open() = RoomStateStore(Room.databaseBuilder(context, GoEloDatabase::class.java, name)
            .addMigrations(GoEloDatabase.MIGRATION_1_2).allowMainThreadQueries().build())
        val migrated = open()
        try {
            val old = migrated.read()
            assertEquals(2010.0, old.state.ratingOf("local"), 0.0)
            assertEquals("local",old.state.matches.single().playerId)
            assertEquals(2L,old.revision)
            ProfileService(migrated).add("b","乙",2010.0,old.revision)
            MatchService(migrated).recordKnown("pair","b",Outcome.WIN,2,"UTC","local")
            ProfileService(migrated).select("b",migrated.read().revision)
        } finally { migrated.close() }
        val reopened=open()
        try {
            val s=reopened.read().state
            assertEquals("b",s.profile!!.id)
            assertEquals(2020.0,s.ratingOf("local"),1e-8)
            assertEquals(2000.0,s.ratingOf("b"),1e-8)
            assertEquals(2,s.matches.size)
            val saved=reopened.read()
            assertTrue(runCatching { reopened.update(saved.revision) {
                it.copy(matches=it.matches.map { m -> if(m.id=="pair") m.copy(opponentEloAfter=900.0) else m })
            } }.isFailure)
            assertEquals(saved,reopened.read())
        } finally { reopened.close(); context.deleteDatabase(name) }
    }
}
