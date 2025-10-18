package id.my.matahati.absensi.data

data class ApiResponse(
    val user_id: Int,
    val count: Int,
    val data: List<AbsensiLogRemote>
)
