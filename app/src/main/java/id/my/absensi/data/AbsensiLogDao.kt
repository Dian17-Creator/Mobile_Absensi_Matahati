package id.my.matahati.absensi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AbsensiLogDao {

    @Query("SELECT * FROM absensi_log WHERE user_id = :userId ORDER BY waktu DESC")
    suspend fun getLogsForUser(userId: Int): List<AbsensiLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AbsensiLog>)
}
