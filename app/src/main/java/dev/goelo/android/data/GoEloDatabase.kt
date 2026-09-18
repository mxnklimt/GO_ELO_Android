package dev.goelo.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProfileEntity::class, MatchEntity::class, MetaEntity::class], version = 2, exportSchema = true)
abstract class GoEloDatabase : RoomDatabase() {
    abstract fun stateDao(): StateDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN playerId TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE matches ADD COLUMN opponentPlayerId TEXT")
                db.execSQL("ALTER TABLE matches ADD COLUMN opponentEloAfter REAL")
                db.execSQL("ALTER TABLE meta ADD COLUMN activePlayerId TEXT NOT NULL DEFAULT 'local'")
            }
        }
    }
}
