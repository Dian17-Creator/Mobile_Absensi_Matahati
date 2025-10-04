package id.my.matahati.absensi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: UserSchedule)

    @Query("SELECT * FROM tuserschedule WHERE nuserid = :userId ORDER BY dwork")
    suspend fun getSchedulesForUser(userId: Int): List<UserSchedule>

    @Query("SELECT * FROM tuserschedule WHERE nuserid = :userId AND dwork = :date LIMIT 1")
    suspend fun getScheduleForDate(userId: Int, date: String): UserSchedule?


}
