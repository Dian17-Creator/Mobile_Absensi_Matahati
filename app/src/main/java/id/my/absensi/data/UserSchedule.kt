package id.my.matahati.absensi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tuserschedule")
data class UserSchedule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "nid")
    val nid: Int = 0,

    @ColumnInfo(name = "nuserid")
    val nuserid: Int,

    @ColumnInfo(name = "dwork")
    val dwork: String,    // "YYYY-MM-DD"

    @ColumnInfo(name = "dstart")
    val dstart: String,   // "08:00:00"

    @ColumnInfo(name = "dend")
    val dend: String,     // "16:00:00"

    @ColumnInfo(name = "nidsched")
    val nidsched: Int,

    @ColumnInfo(name = "cschedname")
    val cschedname: String
)
