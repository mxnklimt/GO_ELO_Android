package dev.goelo.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProfileEntity::class, MatchEntity::class, MetaEntity::class, DogIdEntity::class], version = 3, exportSchema = true)
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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS dog_ids (id TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}
