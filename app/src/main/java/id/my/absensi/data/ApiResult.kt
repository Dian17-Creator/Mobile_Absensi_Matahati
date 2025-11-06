package id.my.matahati.absensi.data

data class ApiResult(
    val success: Boolean,
    val count: Int,
    val data: List<AktivitasResponse>
)
