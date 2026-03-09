package id.my.matahati.absensi.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "agenda")
data class UserAgenda(

    @PrimaryKey
    val id: Long,

    @SerializedName("user_id")
    val userId: Long?,

    val title: String,
    val description: String?,

    @SerializedName("start_at")
    val startAt: String,

    @SerializedName("end_at")
    val endAt: String?,

    @SerializedName("is_all_day")
    val isAllDay: Boolean,

    val color: String?,
    val status: String
)


