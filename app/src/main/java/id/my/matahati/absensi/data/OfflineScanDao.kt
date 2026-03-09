package id.my.matahati.absensi.data

import androidx.room.*

// DAO = Data Access Object (interface query ke database)
@Dao
interface OfflineScanDao {
    @Insert
    suspend fun insert(scan: OfflineScan)

    @Query("SELECT * FROM offline_scans WHERE status = 'pending'")
    suspend fun getPendingScans(): List<OfflineScan>

    @Update
    suspend fun update(scan: OfflineScan)

    @Query("DELETE FROM offline_scans WHERE status = 'accepted'")
    suspend fun deleteAccepted()

    @Delete
    suspend fun delete(scan: OfflineScan)

    @Query("SELECT * FROM offline_scans")
    suspend fun getAll(): List<OfflineScan>
}

