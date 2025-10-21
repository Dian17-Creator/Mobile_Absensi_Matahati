package id.my.matahati.absensi.data

data class AbsensiApiModel(
    val nid: Int,
    val nuserId: Int,
    val dscanned: String,
    val nlat: String?,
    val nlng: String?,
    val nadminid: Int?
)
