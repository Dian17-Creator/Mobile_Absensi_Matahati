package id.my.matahati.absensi.data

data class CompanyUpdateResponse(
    val success: Boolean,
    val message: String,
    val data: CompanyUpdateData
)

data class CompanyUpdateData(
    val company: Company
)

data class Company(
    val cname: String,
    val cemail: String
)
