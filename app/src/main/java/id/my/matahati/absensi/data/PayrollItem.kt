package id.my.matahati.absensi.data

data class PayrollItem(

    val id: Int?,

    val user_id: Int?,

    val department_id: String?,

    val user_name: String?,

    val jabatan: String?,

    val jumlah_masuk: Int?,

    val gaji: String?,

    val gaji_pokok: String?,

    val tunjangan_makan: String?,

    val tunjangan_jabatan: String?,

    val tunjangan_transport: String?,

    val tunjangan_luar_kota: String?,

    val tunjangan_masa_kerja: String?,

    val tunjangan_backup: String?,

    val gaji_lembur: String?,

    val bonus_kehadiran: String?,

    val potongan_lain: String?,

    val potongan_tabungan: String?,

    val potongan_keterlambatan: String?,

    val total_gaji: String?,

    val status: String?,

    val pdf_url: String?,

    val note: String?,

    val reasonedit: String?
)
