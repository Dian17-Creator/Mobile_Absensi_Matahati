package id.my.matahati.absensi.data

data class SalaryResponse(
    val success: Boolean,
    val data: List<SalaryItem>
)

data class SalaryItem(
    val id: Int,
    val period_year: Int,
    val period_month: Int,
    val jabatan: String,
    val jumlah_masuk: Int,
    val status: String,
    val note: String?,
    val total_gaji: Double,

    val penghasilan: List<SalaryComponent>,
    val potongan: List<SalaryComponent>
)

