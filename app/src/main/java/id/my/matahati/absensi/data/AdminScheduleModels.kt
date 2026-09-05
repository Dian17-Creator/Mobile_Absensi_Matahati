package id.my.matahati.absensi.data

data class MasterScheduleListResponse(
    val success: Boolean,
    val data: List<MasterScheduleItem>
)

data class MasterScheduleItem(
    val nid: Int,
    val cname: String,
    val ctype: String?,
    val ctotal: Int?,
    val dstart: String?,
    val dend: String?,
    val dstart2: String?,
    val dend2: String?
)

data class AdminScheduleListResponse(
    val success: Boolean,
    val data: List<AdminScheduleItem>
)

data class AdminScheduleItem(
    val nid: Int,
    val nuserid: Int,
    val dwork: String,
    val dstart: String?,
    val dend: String?,
    val dstart2: String?,
    val dend2: String?,
    val nidsched: Int,
    val cschedname: String?,
    val user: UserInfo?,
    val master_schedule: MasterScheduleItem?
)

data class UserInfo(
    val nid: Int,
    val cname: String,
    val department: DepartmentItem?
)
