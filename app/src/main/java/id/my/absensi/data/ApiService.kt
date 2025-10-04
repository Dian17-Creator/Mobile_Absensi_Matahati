package id.my.matahati.absensi.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {
    @GET("schedule.php?api=1")
    suspend fun getUserSchedules(
        @Query("userid") userId: Int
    ): List<UserSchedule>
}
