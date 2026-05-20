package id.my.matahati.absensi.data

data class PendingFaceUser(
    val nid: Int,
    val cname: String,
    val niddept: Int,
    val department: String?,
    val faces: List<FaceItem>
)