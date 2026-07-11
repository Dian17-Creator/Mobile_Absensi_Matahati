package id.my.matahati.absensi.data

data class UserStoreRequest(
    val email: String,
    val name: String,
    val cfullname: String?,
    val password: String,
    val niddept: Int,

    val niddeptpayroll: Int? = null,
    val cmailaddress: String? = null,
    val caccnumber: String? = null,
    val cphone: String? = null,
    val cktp: String? = null,
    val finger_id: Int? = null,
    val dtanggalmasuk: String? = null,
    val rekening_id: Int? = null,
    val bank: String? = null,
    val fnotif: Int = 0,
    val role: String = "crew"
)