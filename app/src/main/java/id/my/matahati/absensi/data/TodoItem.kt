package id.my.matahati.absensi.data

data class TodoItem(
    val nid: Int,
    val nid_peminta: Int,
    val ndep_tujuan: Int,
    val departemen_tujuan: String,
    val cpermintaan: String,
    val dminta: String,
    val nid_pelaksana: Int?,
    val nama_peminta: String,
    val nama_pelaksana: String?,
    val fselesai: Boolean,
    val dselesai: String?
)
