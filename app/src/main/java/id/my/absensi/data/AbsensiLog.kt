package id.my.matahati.absensi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "absensi_log")
data class AbsensiLog(
    @PrimaryKey val id: Int,
    val user_id: Int,
    val waktu: String,
    val scan: String? = null,
    val approved_by: String? = null
)
