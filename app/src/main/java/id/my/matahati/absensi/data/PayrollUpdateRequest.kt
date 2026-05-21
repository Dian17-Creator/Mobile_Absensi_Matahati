package id.my.matahati.absensi.data

data class PayrollUpdateRequest(

    val jumlah_masuk: Int,

    val gaji_harian: String,
    val gaji_pokok: String,

    val tunjangan_makan: String,
    val tunjangan_jabatan: String,
    val tunjangan_transport: String,
    val tunjangan_luar_kota: String,
    val tunjangan_masa_kerja: String,
    val tunjangan_backup: String,

    val gaji_lembur: String,
    val bonus_kehadiran: String,

    val tabungan_diambil: String,

    val potongan_lain: String,
    val potongan_tabungan: String,
    val potongan_keterlambatan: String,

    val note: String?,
    val reasonedit: String?
)