package id.my.matahati.absensi.data

data class SalaryResponse(
    val success: Boolean,
    val data: List<SalaryItem>
)

data class SalaryItem(

    val period_year: Int,
    val period_month: Int,
    val jabatan: String,
    val jumlah_masuk: Int,

    val gaji_harian: String?,
    val gaji_pokok: String?,
    val tunjangan_makan: String?,
    val tunjangan_jabatan: String?,
    val tunjangan_transport: String?,
    val tunjangan_luar_kota: String?,
    val tunjangan_masa_kerja: String?,
    val gaji_lembur: String?,
    val tabungan_diambil: String?,

    val potongan_lain: String?,
    val potongan_tabungan: String?,
    val potongan_keterlambatan: String?,

    val total_gaji: String?,

    val note: String?,
    val user_note: String?,
    val status: String
)

