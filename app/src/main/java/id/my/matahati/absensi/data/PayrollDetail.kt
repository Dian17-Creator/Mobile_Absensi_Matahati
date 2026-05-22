package id.my.matahati.absensi.data

data class PayrollDetail(
    val id: Int,

    val user_name: String?,
    val jabatan: String?,

    val jumlah_masuk: Int,

    val gaji_harian: Double,
    val gaji_pokok: Double,

    val tunjangan_makan: Double,
    val tunjangan_jabatan: Double,
    val tunjangan_transport: Double,
    val tunjangan_luar_kota: Double,
    val tunjangan_masa_kerja: Double,
    val tunjangan_backup: Double,

    val gaji_lembur: Double,
    val bonus_kehadiran: Double,

    val tabungan_diambil: Double,

    val potongan_lain: Double,
    val potongan_tabungan: Double,
    val potongan_keterlambatan: Double,

    val total_gaji: Double,

    val note: String?,
    val reasonedit: String?
)
