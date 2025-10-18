package id.my.matahati.absensi.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {
    @GET("api/schedule/{userId}")
    suspend fun getUserSchedules(
        @Path("userId") userId: Int
    ): List<UserSchedule>


    @GET("api/logs/{userId}")
    suspend fun getLogsByUser(@Path("userId") userId: Int): ApiResponse

}
