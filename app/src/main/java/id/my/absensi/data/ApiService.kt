package id.my.matahati.absensi.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Response

interface ApiService {

    // Ambil jadwal user
    @GET("api/schedule/{userId}")
    suspend fun getUserSchedules(
        @Path("userId") userId: Int
    ): Response<List<UserSchedule>>

    // Ambil log user
    @GET("api/logs/{userId}")
    suspend fun getLogsByUser(
        @Path("userId") userId: Int
    ): Response<List<AbsensiApiModel>>
}
