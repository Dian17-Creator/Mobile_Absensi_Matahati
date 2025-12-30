package id.my.matahati.absensi.data

import id.my.absensi.data.ScheduleApiResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Field

interface ApiService {

    // Ambil jadwal user
    @GET("api/schedule/{userId}")
    suspend fun getUserSchedules(
        @Path("userId") userId: Int
    ): Response<ScheduleApiResponse>

    // Ambil log user
    @GET("api/logs/{userId}")
    suspend fun getLogsByUser(
        @Path("userId") userId: Int
    ): Response<List<AbsensiLogRemote>>

    @GET("get_aktivitas.php")
    suspend fun getAktivitas(
        @Query("type") type: String,
        @Query("userId") userId: Int
    ): Response<ApiResult>

    @FormUrlEncoded
    @POST("approval_list.php")
    suspend fun getApprovalList(
        @Field("type") type: String,
        @Field("user_id") userId: Int
    ): Response<ApprovalResponse>

    @FormUrlEncoded
    @POST("approval_action.php")
    suspend fun approvalAction(
        @Field("user_id") userId: Int,
        @Field("id") id: Int,
        @Field("type") type: String,
        @Field("action") action: String
    ): Response<ApiResponse>
}
