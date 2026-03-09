package id.my.matahati.absensi.data

import java.time.LocalDate

import com.google.gson.annotations.SerializedName

data class UserContract(
    @SerializedName("contract_id")
    val contractId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("end_date")
    val endDate: String,

    @SerializedName("term_month")
    val termMonth: Int,

    @SerializedName("contract_type")
    val contractType: String,

    val status: String
)
