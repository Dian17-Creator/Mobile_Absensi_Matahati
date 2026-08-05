package id.my.matahati.absensi.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

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

    //    Kontrak kerja user
    @GET
    suspend fun getUserContract(
        @Url url: String
    ): Response<UserContractResponse>

    //Gaji user
    @GET("api/user/gaji/{userId}")
    suspend fun getUserSalary(
        @Path("userId") userId: Int,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): Response<SalaryResponse>

    @FormUrlEncoded
    @POST("api/user/gaji/status")
    suspend fun updateSalaryStatus(
        @Field("user_id") userId: Int,
        @Field("year") year: Int,
        @Field("month") month: Int,
        @Field("status") status: String,
        @Field("note") note: String?
    ): Response<Unit>

    @GET("api/agenda/{month}")
    suspend fun getAgenda(
        @Path("month") month: String
    ): List<UserAgenda>

    @FormUrlEncoded
    @POST("api/save-token")
    suspend fun saveDeviceToken(
        @Field("user_id") userId: Int,
        @Field("fcm_token") token: String
    ): Response<ApiResponse>

    @POST("api/user/store")
    suspend fun storeUser(
        @Body request: UserStoreRequest
    ): Response<StoreUserResponse>

    @GET("api/department/list")
    suspend fun getDepartments(): Response<DepartmentResponse>

    @GET("api/bank/list")
    suspend fun getBankList(): Response<BankResponse>

    @GET("api/mandiri/rekening")
    suspend fun getMandiriRekening(): Response<RekeningResponse>

    @GET("api/face-approval/pending")
    suspend fun getPendingFaceList(
        @Query("approver_id") approverId: Int
    ): Response<FacePendingResponse>

    @FormUrlEncoded
    @POST("api/face-approval/{id}/approve")
    suspend fun approveFace(
        @Path("id") userId: Int,
        @Field("approver_id") approverId: Int
    ): ApiMessageResponse

    @FormUrlEncoded
    @POST("api/face-approval/{id}/reject")
    suspend fun rejectFace(
        @Path("id") userId: Int,
        @Field("approver_id") approverId: Int
    ): ApiMessageResponse

    @GET("api/user/gaji/list")
    suspend fun getPayrollList(

        @Query("department_id")
        departmentId: String?,

        @Query("year")
        year: Int,

        @Query("month")
        month: Int

    ): Response<PayrollListResponse>

    @GET("api/user/gaji/{id}/detail")
    suspend fun getPayrollDetail(
        @Path("id") id: Int
    ): Response<PayrollDetailResponse>

    @POST("api/user/gaji/{id}/update")
    suspend fun updatePayroll(
        @Path("id") id: Int,
        @Body request: PayrollUpdateRequest
    ): Response<ApiMessageResponse>

    @GET("api/todo")
    suspend fun getTodoList(
        @Query("user_id") userId: Int
    ): Response<TodoResponse>

    @FormUrlEncoded
    @POST("api/todo/complete/{id}")
    suspend fun completeTodo(
        @Path("id") taskId: Int,
        @Field("user_id") userId: Int
    ): Response<ApiMessageResponse>
}
