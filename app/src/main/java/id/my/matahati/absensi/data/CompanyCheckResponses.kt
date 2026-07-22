package id.my.matahati.absensi.data

data class CompanyCheckResponses(
    val success: Boolean,
    val data: CompanyCheckData
)

data class CompanyCheckData(
    val name_exists: Boolean,
    val domain_exists: Boolean
)
