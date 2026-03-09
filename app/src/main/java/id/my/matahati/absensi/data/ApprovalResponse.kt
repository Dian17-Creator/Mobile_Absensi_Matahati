package id.my.matahati.absensi.data

import id.my.matahati.absensi.data.ApprovalItem

data class ApprovalResponse(
    val success: Boolean,
    val data: List<ApprovalItem>
)