package id.my.matahati.absensi.data

import id.my.matahati.absensi.data.UserContract

data class UserContractResponse(
    val success: Boolean,
    val message: String,
    val data: UserContract?
)
