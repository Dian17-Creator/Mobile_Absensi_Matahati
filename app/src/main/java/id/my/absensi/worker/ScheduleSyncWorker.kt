package id.my.matahati.absensi.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.data.ApiClient
import id.my.matahati.absensi.data.UserSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ScheduleSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val userId = inputData.getInt("USER_ID", -1)
        if (userId == -1) return@withContext Result.failure()

        if (!isInternetAvailable()) {
            return@withContext Result.retry()
        }

        try {
            val api = ApiClient.apiService
            val dao = MyApp.db.userScheduleDao()

            val response = api.getUserSchedules(userId)

            if (!response.isSuccessful) {
                return@withContext Result.retry()
            }

            val body = response.body() ?: return@withContext Result.success()
            if (!body.success) return@withContext Result.success()

            // 🔥 Convert API → Entity
            val schedules = body.data.flatMap { day ->
                day.sessions.map { session ->
                    UserSchedule(
                        nid = 0,
                        nuserid = userId,
                        dwork = day.date,
                        dstart = session.start,
                        dend = session.end,
                        nidsched = 0,
                        cschedname = day.shiftName
                    )
                }
            }

            if (schedules.isNotEmpty()) {
                dao.deleteAllForUser(userId)
                schedules.forEach { dao.insert(it) }
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = applicationContext.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
