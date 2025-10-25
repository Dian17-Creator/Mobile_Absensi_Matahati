package id.my.matahati.absensi.data

import com.google.gson.annotations.SerializedName

data class AbsensiLogRemote(
    @SerializedName("nid") val nid: Int,
    @SerializedName("nuserId") val nuserId: Int,
    @SerializedName("dscanned") val dscanned: String,
    @SerializedName("nlat") val nlat: String?,
    @SerializedName("nlng") val nlng: String?,
    @SerializedName("nadminid") val nadminid: Int?,
    @SerializedName("type_absen") val typeAbsensi: String
)
