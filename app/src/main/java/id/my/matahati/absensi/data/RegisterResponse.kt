package id.my.matahati.absensi.data

data class RegisterResponse(
    val success : Boolean,
    val message : String,
    val data : RegisterData
)

data class RegisterData(
    val company: String,
    val email: String
)
