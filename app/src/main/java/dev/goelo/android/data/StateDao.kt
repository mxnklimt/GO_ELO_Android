package dev.goelo.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StateDao {
    @Query("SELECT * FROM profile WHERE id = 'local' LIMIT 1") suspend fun profile(): ProfileEntity?
    @Query("SELECT * FROM profile ORDER BY id ASC") suspend fun profiles(): List<ProfileEntity>
    @Query("SELECT * FROM matches ORDER BY orderIndex ASC") suspend fun matches(): List<MatchEntity>
    @Query("SELECT * FROM meta WHERE id = 0 LIMIT 1") suspend fun meta(): MetaEntity?
    @Query("SELECT revision FROM meta WHERE id = 0 LIMIT 1") fun observeRevision(): Flow<Long?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProfile(entity: ProfileEntity)
    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMatches(entities: List<MatchEntity>)
    @Query("DELETE FROM matches") suspend fun clearMatches()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeta(entity: MetaEntity)
}
