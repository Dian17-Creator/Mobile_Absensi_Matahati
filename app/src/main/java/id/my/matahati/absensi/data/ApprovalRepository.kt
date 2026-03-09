package id.my.matahati.absensi.data

class ApprovalRepository {

    private val api = RetrofitClient.instance

    suspend fun getList(
        type: String,
        userId: Int
    ) = api.getApprovalList(type, userId)

    suspend fun action(
        userId: Int,
        id: Int,
        type: String,
        action: String
    ) = api.approvalAction(
        userId = userId,
        id = id,
        type = type,
        action = action
    )
}
