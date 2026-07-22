package id.my.matahati.absensi.data

data class CompanyResponse(
    val success: Boolean,
    val data: CompanyData
)

data class CompanyData(
    val cname: String,
    val cemail: String
)
