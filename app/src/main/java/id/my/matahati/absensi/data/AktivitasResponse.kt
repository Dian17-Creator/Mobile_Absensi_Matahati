package id.my.matahati.absensi.data

import com.google.gson.annotations.SerializedName

data class AktivitasResponse(
    @SerializedName("nid") val nid: Int,
    @SerializedName("nuserid") val nuserid: Int,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("cplacename") val cplacename: String?,
    @SerializedName("creason") val creason: String?,
    @SerializedName("cstatus") val cstatus: String?,
    @SerializedName("chrdstat") val chrdstat: String?
)
