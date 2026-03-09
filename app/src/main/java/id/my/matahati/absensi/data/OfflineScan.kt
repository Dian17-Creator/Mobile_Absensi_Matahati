package id.my.matahati.absensi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_scans")
data class OfflineScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val token: String,
    val userId: Int,
    val lat: Double,
    val lng: Double,
    val cplacename: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "accepted"
)
