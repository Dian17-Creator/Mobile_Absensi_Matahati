package id.my.matahati.absensi.data

data class FaceApprovalResponse(
    val success: Boolean,
    val data: List<PendingFaceUser>
)
