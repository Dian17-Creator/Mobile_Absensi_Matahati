package id.my.matahati.absensi.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field

interface ApprovalApi {

    // =======================
    // LIST APPROVAL
    // =======================
    @GET("approval_list.php")
    suspend fun getApprovalList(
        @Query("type") type: String,
        @Query("user_id") userId: Int
    ): Response<ApprovalResponse>

    // =======================
    // ACTION APPROVE / REJECT
    // =======================
    @FormUrlEncoded
    @POST("approval_action.php")
    suspend fun approvalAction(
        @Field("user_id") userId: Int,
        @Field("id") id: Int,
        @Field("type") type: String,
        @Field("action") action: String
    ): Response<ApiActionResponse>
}
