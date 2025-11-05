package id.my.absensi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import id.my.matahati.absensi.data.OfflineManualAbsen

@Dao
interface OfflineManualAbsenDao {
    @Insert
    suspend fun insert(data: OfflineManualAbsen)

    @Query("SELECT * FROM offline_manual_absen")
    suspend fun getAll(): List<OfflineManualAbsen>

    @Delete
    suspend fun delete(data: OfflineManualAbsen)
}
