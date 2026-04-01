package id.my.matahati.absensi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_izin")
data class OfflineIzin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: String,
    val coordinate: String,
    val placeName: String,
    val category: String,
    val reason: String,
    val photoBase64: String,

    // 🔥 TAMBAHAN DEVICE INFO
    val model: String,
    val manufacturer: String,
    val osVersion: String,

    val createdAt: Long = System.currentTimeMillis()
)
