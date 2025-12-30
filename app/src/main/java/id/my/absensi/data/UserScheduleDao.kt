package id.my.matahati.absensi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserScheduleDao {

    /**
     * Simpan satu jadwal (insert or replace)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: UserSchedule)

    /**
     * Simpan banyak jadwal sekaligus (lebih cepat daripada forEach)
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<UserSchedule>)

    /**
     * Ambil semua jadwal milik user
     */
    @Query("SELECT * FROM tuserschedule WHERE nuserid = :userId ORDER BY dwork")
    suspend fun getSchedulesForUser(userId: Int): List<UserSchedule>

    /**
     * Ambil jadwal dalam rentang ±7 hari dari tanggal sekarang
     */
    @Query("""
        SELECT * FROM tuserschedule 
        WHERE nuserid = :userId 
          AND dwork BETWEEN :startDate AND :endDate 
        ORDER BY dwork
    """)
    suspend fun getSchedulesInRange(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<UserSchedule>

    /**
     * Hapus data jadwal di luar rentang (misal > 30 hari lalu)
     * agar database tetap ringan tanpa kehilangan data aktif.
     */
    @Query("""
        DELETE FROM tuserschedule 
        WHERE nuserid = :userId 
          AND (dwork < :startDate OR dwork > :endDate)
    """)
    suspend fun deleteOutOfRange(
        userId: Int,
        startDate: String,
        endDate: String
    )

    /**
     * Ambil 1 jadwal spesifik pada tanggal tertentu
     */
    @Query("""
    SELECT * FROM tuserschedule 
    WHERE nuserid = :userId 
      AND dwork = :date 
    ORDER BY dstart
    """)
    suspend fun getSchedulesForDate(
        userId: Int,
        date: String
    ): List<UserSchedule>

    @Query("DELETE FROM tuserschedule WHERE nuserid = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
